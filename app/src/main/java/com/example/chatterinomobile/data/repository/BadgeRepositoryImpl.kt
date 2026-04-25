package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.local.BadgeDiskCache
import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.remote.api.SevenTvCosmeticsApi
import com.example.chatterinomobile.data.remote.api.TwitchHelixApi
import com.example.chatterinomobile.data.remote.mapper.toDomain
import com.example.chatterinomobile.data.remote.mapper.twitchBadgeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * In-memory badge cache backed by [BadgeDiskCache] for instant cold-start
 * restores.
 *
 * Reads go through unsynchronized `HashMap`s because badges are looked up
 * from the render loop on every message — taking a mutex there would be
 * disastrous. Writes are serialized by [writeMutex] so concurrent channel
 * switches don't trample each other.
 *
 * The slight read-side race (a channel badge appearing mid-write) is fine:
 * the worst case is one frame with an older badge image.
 *
 * Same stale-while-revalidate strategy as [EmoteRepositoryImpl]: we serve the
 * last successful disk snapshot immediately and refresh in the background
 * when it's past TTL. Channel-scoped badges (custom sub/bit badges) expire
 * out of memory after [CHANNEL_IDLE_EVICT_MILLIS] of no lookups so session
 * memory stays bounded.
 */
class BadgeRepositoryImpl(
    private val helixApi: TwitchHelixApi,
    private val sevenTvCosmeticsApi: SevenTvCosmeticsApi,
    private val diskCache: BadgeDiskCache,
    scopeOverride: CoroutineScope? = null
) : BadgeRepository {

    private val bgScope = scopeOverride ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    // key = "$setId/$version"
    private val globalTwitchBadges = HashMap<String, Badge>()

    // channelId -> (key -> Badge)
    private val channelTwitchBadges = HashMap<String, HashMap<String, Badge>>()

    // twitchUserId -> list of SEVENTV badges
    private val sevenTvBadgesByUser = HashMap<String, MutableList<Badge>>()

    private var globalLoadedAtMillis = 0L
    private val channelLoadedAtMillis = HashMap<String, Long>()
    private val channelLastAccessedAtMillis = HashMap<String, Long>()
    private val writeMutex = Mutex()

    init {
        bgScope.launch { idleEvictionLoop() }
    }

    override suspend fun loadGlobalBadges() {
        val globalSnap = diskCache.readGlobalTwitch()
        val userSnap = diskCache.readSevenTvByUser()

        if (globalSnap != null && globalLoadedAtMillis == 0L) {
            writeMutex.withLock {
                if (globalLoadedAtMillis == 0L) {
                    globalTwitchBadges.clear()
                    globalTwitchBadges.putAll(globalSnap.entries)
                    sevenTvBadgesByUser.clear()
                    if (userSnap != null) {
                        for ((userId, badges) in userSnap.entries) {
                            sevenTvBadgesByUser[userId] = badges.toMutableList()
                        }
                    }
                    globalLoadedAtMillis = globalSnap.savedAtEpochMillis
                }
            }
        }

        val needsRefresh = globalSnap == null ||
            System.currentTimeMillis() - globalSnap.savedAtEpochMillis >= GLOBAL_CACHE_TTL_MILLIS

        if (!needsRefresh) return

        val refreshJob = bgScope.launch { refreshGlobalFromNetwork() }
        if (globalSnap == null) refreshJob.join()
    }

    private suspend fun refreshGlobalFromNetwork() = withContext(Dispatchers.IO) {
        coroutineScope {
            val twitchDeferred = async {
                runCatching { helixApi.getGlobalBadges() }.getOrElse { emptyList() }
            }
            val cosmeticsDeferred = async {
                runCatching { sevenTvCosmeticsApi.getCosmetics() }.getOrNull()
            }
            val twitchSets = twitchDeferred.await()
            val cosmetics = cosmeticsDeferred.await()

            val freshGlobal = HashMap<String, Badge>()
            for (set in twitchSets) {
                for (version in set.versions) {
                    freshGlobal[twitchBadgeKey(set.setId, version.id)] =
                        version.toDomain(set.setId)
                }
            }
            val freshUsers = HashMap<String, MutableList<Badge>>()
            if (cosmetics != null) {
                for (badgeDto in cosmetics.badges) {
                    val badge = badgeDto.toDomain()
                    for (userId in badgeDto.users) {
                        freshUsers.getOrPut(userId) { mutableListOf() }.add(badge)
                    }
                }
            }

            if (freshGlobal.isEmpty() && freshUsers.isEmpty()) return@coroutineScope

            writeMutex.withLock {
                if (freshGlobal.isNotEmpty()) {
                    globalTwitchBadges.clear()
                    globalTwitchBadges.putAll(freshGlobal)
                }
                sevenTvBadgesByUser.clear()
                for ((uid, list) in freshUsers) sevenTvBadgesByUser[uid] = list
                globalLoadedAtMillis = System.currentTimeMillis()
            }

            if (freshGlobal.isNotEmpty()) diskCache.writeGlobalTwitch(freshGlobal)
            diskCache.writeSevenTvByUser(freshUsers.mapValues { it.value.toList() })
        }
    }

    override suspend fun loadChannelBadges(channelId: String) {
        channelLastAccessedAtMillis[channelId] = System.currentTimeMillis()

        val snapshot = diskCache.readChannel(channelId)
        if (snapshot != null && !channelTwitchBadges.containsKey(channelId)) {
            writeMutex.withLock {
                if (!channelTwitchBadges.containsKey(channelId)) {
                    channelTwitchBadges[channelId] = HashMap(snapshot.entries)
                    channelLoadedAtMillis[channelId] = snapshot.savedAtEpochMillis
                }
            }
        }

        val needsRefresh = snapshot == null ||
            System.currentTimeMillis() - snapshot.savedAtEpochMillis >= CHANNEL_CACHE_TTL_MILLIS

        if (!needsRefresh) return

        val refreshJob = bgScope.launch { refreshChannelFromNetwork(channelId) }
        if (snapshot == null) refreshJob.join()
    }

    private suspend fun refreshChannelFromNetwork(channelId: String) {
        val sets = withContext(Dispatchers.IO) {
            runCatching { helixApi.getChannelBadges(channelId) }.getOrElse { emptyList() }
        }
        val bucket = HashMap<String, Badge>()
        for (set in sets) {
            for (version in set.versions) {
                bucket[twitchBadgeKey(set.setId, version.id)] = version.toDomain(set.setId)
            }
        }
        if (bucket.isEmpty()) return

        writeMutex.withLock {
            channelTwitchBadges[channelId] = bucket
            channelLoadedAtMillis[channelId] = System.currentTimeMillis()
        }
        diskCache.writeChannel(channelId, bucket)
    }

    override fun findTwitchBadge(setId: String, version: String, channelId: String?): Badge? {
        val key = twitchBadgeKey(setId, version)
        if (channelId != null) {
            channelTwitchBadges[channelId]?.get(key)?.let {
                channelLastAccessedAtMillis[channelId] = System.currentTimeMillis()
                return it
            }
        }
        return globalTwitchBadges[key]
    }

    override fun findThirdPartyBadges(twitchUserId: String): List<Badge> =
        sevenTvBadgesByUser[twitchUserId].orEmpty()

    override fun clearCache(channelId: String?) {
        if (channelId == null) {
            globalTwitchBadges.clear()
            sevenTvBadgesByUser.clear()
            channelTwitchBadges.clear()
            channelLoadedAtMillis.clear()
            channelLastAccessedAtMillis.clear()
            globalLoadedAtMillis = 0L
            return
        }

        channelTwitchBadges.remove(channelId)
        channelLoadedAtMillis.remove(channelId)
        channelLastAccessedAtMillis.remove(channelId)
    }

    private suspend fun idleEvictionLoop() {
        while (true) {
            delay(IDLE_EVICTION_TICK_MILLIS)
            val cutoff = System.currentTimeMillis() - CHANNEL_IDLE_EVICT_MILLIS
            val victims = writeMutex.withLock {
                channelLastAccessedAtMillis
                    .filterValues { it < cutoff }
                    .keys
                    .toList()
            }
            if (victims.isEmpty()) continue
            writeMutex.withLock {
                for (channelId in victims) {
                    channelTwitchBadges.remove(channelId)
                    channelLoadedAtMillis.remove(channelId)
                    channelLastAccessedAtMillis.remove(channelId)
                }
            }
        }
    }

    companion object {
        private const val GLOBAL_CACHE_TTL_MILLIS = 6L * 60L * 60L * 1000L
        private const val CHANNEL_CACHE_TTL_MILLIS = 30L * 60L * 1000L
        private const val CHANNEL_IDLE_EVICT_MILLIS = 15L * 60L * 1000L
        private const val IDLE_EVICTION_TICK_MILLIS = 60L * 1000L
    }
}
