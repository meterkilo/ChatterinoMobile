package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.local.BadgeDiskCache
import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.remote.api.ChatterinoApi
import com.example.chatterinomobile.data.remote.api.FfzApi
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

class BadgeRepositoryImpl(
    private val helixApi: TwitchHelixApi,
    private val sevenTvCosmeticsApi: SevenTvCosmeticsApi,
    private val ffzApi: FfzApi,
    private val chatterinoApi: ChatterinoApi,
    private val diskCache: BadgeDiskCache,
    scopeOverride: CoroutineScope? = null
) : BadgeRepository {

    private val bgScope = scopeOverride ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val globalTwitchBadges = HashMap<String, Badge>()

    private val channelTwitchBadges = HashMap<String, HashMap<String, Badge>>()

    private val thirdPartyBadgesByUser = HashMap<String, MutableList<Badge>>()
    private val ffzBadgesById = HashMap<Int, Badge>()
    private val sevenTvUserBadgeLookups = HashSet<String>()

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
                    thirdPartyBadgesByUser.clear()
                    if (userSnap != null) {
                        for ((userId, badges) in userSnap.entries) {
                            thirdPartyBadgesByUser[userId] = badges.toMutableList()
                        }
                    }
                    globalLoadedAtMillis = globalSnap.savedAtEpochMillis
                }
            }
        }

        val needsRefresh = globalSnap == null ||
            System.currentTimeMillis() - globalSnap.savedAtEpochMillis >= GLOBAL_CACHE_TTL_MILLIS

        val refreshJob = bgScope.launch { refreshGlobalFromNetwork() }
        if (globalSnap == null || needsRefresh) refreshJob.join()
    }

    private suspend fun refreshGlobalFromNetwork() = withContext(Dispatchers.IO) {
        coroutineScope {
            val twitchDeferred = async {
                runCatching { helixApi.getGlobalBadges() }.getOrElse { emptyList() }
            }
            val ffzDeferred = async {
                runCatching { ffzApi.getBadges() }.getOrNull()
            }
            val chatterinoDeferred = async {
                runCatching { chatterinoApi.getBadges() }.getOrNull()
            }
            val twitchSets = twitchDeferred.await()
            val ffz = ffzDeferred.await()
            val chatterino = chatterinoDeferred.await()

            val freshGlobal = HashMap<String, Badge>()
            for (set in twitchSets) {
                for (version in set.versions) {
                    freshGlobal[twitchBadgeKey(set.setId, version.id)] =
                        version.toDomain(set.setId)
                }
            }
            val freshUsers = HashMap<String, MutableList<Badge>>()
            if (ffz != null) {
                val freshFfzBadges = ffz.badges.associateBy({ it.id }, { it.toDomain() })
                ffzBadgesById.clear()
                ffzBadgesById.putAll(freshFfzBadges)
                for ((badgeId, userIds) in ffz.users) {
                    val badge = freshFfzBadges[badgeId.toIntOrNull()] ?: continue
                    for (userId in userIds) {
                        freshUsers.getOrPut(userId.toString()) { mutableListOf() }.add(badge)
                    }
                }
            }
            if (chatterino != null) {
                for ((index, badgeDto) in chatterino.badges.withIndex()) {
                    val badge = badgeDto.toDomain(index)
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
                for ((uid, list) in freshUsers) {
                    val bucket = thirdPartyBadgesByUser.getOrPut(uid) { mutableListOf() }
                    for (badge in list) {
                        if (bucket.none { it.id == badge.id }) bucket.add(badge)
                    }
                }
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

        val refreshJob = bgScope.launch { refreshChannelFromNetwork(channelId) }
        if (snapshot == null || needsRefresh) refreshJob.join()
    }

    private suspend fun refreshChannelFromNetwork(channelId: String) = withContext(Dispatchers.IO) {
        val setsDeferred = async {
            runCatching { helixApi.getChannelBadges(channelId) }.getOrElse { emptyList() }
        }
        val ffzRoomDeferred = async {
            runCatching { ffzApi.getRoom(channelId) }.getOrNull()
        }
        val ffzBadgesDeferred = async {
            runCatching { ffzApi.getBadges() }.getOrNull()
        }

        val sets = setsDeferred.await()
        val ffzRoom = ffzRoomDeferred.await()
        val ffzBadges = ffzBadgesDeferred.await()

        val bucket = HashMap<String, Badge>()
        for (set in sets) {
            for (version in set.versions) {
                bucket[twitchBadgeKey(set.setId, version.id)] = version.toDomain(set.setId)
            }
        }

        val channelThirdParty = HashMap<String, MutableList<Badge>>()
        if (ffzRoom != null) {
            val knownBadges = if (ffzBadges != null) {
                ffzBadges.badges.associateBy({ it.id }, { it.toDomain() })
            } else {
                ffzBadgesById.toMap()
            }
            for ((badgeId, userIds) in ffzRoom.room.userBadgeIds) {
                val badge = knownBadges[badgeId.toIntOrNull()] ?: continue
                for (userId in userIds) {
                    channelThirdParty.getOrPut(userId.toString()) { mutableListOf() }.add(badge)
                }
            }
        }
        if (bucket.isEmpty() && channelThirdParty.isEmpty()) return@withContext

        writeMutex.withLock {
            if (bucket.isNotEmpty()) {
                channelTwitchBadges[channelId] = bucket
            }
            if (ffzBadges != null) {
                ffzBadgesById.clear()
                ffzBadgesById.putAll(ffzBadges.badges.associateBy({ it.id }, { it.toDomain() }))
            }
            for ((userId, badges) in channelThirdParty) {
                val bucketForUser = thirdPartyBadgesByUser.getOrPut(userId) { mutableListOf() }
                for (badge in badges) {
                    if (bucketForUser.none { it.id == badge.id }) bucketForUser.add(badge)
                }
            }
            channelLoadedAtMillis[channelId] = System.currentTimeMillis()
        }
        if (bucket.isNotEmpty()) diskCache.writeChannel(channelId, bucket)
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
        thirdPartyBadgesByUser[twitchUserId].orEmpty()

    override suspend fun loadThirdPartyBadgesForUser(twitchUserId: String) {
        if (twitchUserId.isBlank()) return
        writeMutex.withLock {
            if (!sevenTvUserBadgeLookups.add(twitchUserId)) return
        }

        val result = withContext(Dispatchers.IO) {
            runCatching { sevenTvCosmeticsApi.getUserCosmetics(twitchUserId) }
        }
        val style = result.getOrNull()?.data?.users?.userByConnection?.style
        if (result.isFailure) {
            writeMutex.withLock { sevenTvUserBadgeLookups.remove(twitchUserId) }
            return
        }

        val badge = style?.activeBadge?.toDomain() ?: return
        writeMutex.withLock {
            val bucket = thirdPartyBadgesByUser.getOrPut(twitchUserId) { mutableListOf() }
            if (bucket.none { it.id == badge.id }) bucket.add(badge)
        }
    }

    override fun clearCache(channelId: String?) {
        if (channelId == null) {
            globalTwitchBadges.clear()
            thirdPartyBadgesByUser.clear()
            ffzBadgesById.clear()
            channelTwitchBadges.clear()
            channelLoadedAtMillis.clear()
            channelLastAccessedAtMillis.clear()
            sevenTvUserBadgeLookups.clear()
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
