package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.local.EmoteDimensionStore
import com.example.chatterinomobile.data.local.EmoteDiskCache
import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.remote.api.BttvApi
import com.example.chatterinomobile.data.remote.api.FfzApi
import com.example.chatterinomobile.data.remote.api.SevenTvApi
import com.example.chatterinomobile.data.remote.mapper.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EmoteRepositoryImpl(
    private val sevenTvApi: SevenTvApi,
    private val bttvApi: BttvApi,
    private val ffzApi: FfzApi,
    private val diskCache: EmoteDiskCache,
    private val dimensionStore: EmoteDimensionStore,
    scopeOverride: CoroutineScope? = null
) : EmoteRepository {

    private val scope = scopeOverride ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val globalEmotesByName = HashMap<String, Emote>()
    private val channelEmotesByChannelId = HashMap<String, HashMap<String, Emote>>()
    private var globalLoadedAtMillis = 0L
    private val channelLoadedAtMillis = HashMap<String, Long>()
    private val channelLastAccessedAtMillis = HashMap<String, Long>()
    private val inFlightLoads = HashMap<String, Deferred<Map<String, Emote>>>()
    private val mutex = Mutex()

    init {

        scope.launch { dimensionStore.ensureLoaded() }
        scope.launch { idleEvictionLoop() }
    }

    override suspend fun loadEmotesForChannel(channelId: String?) {
        if (channelId == null) {
            loadGlobalWithDiskFallback()
        } else {
            loadChannelWithDiskFallback(channelId)
        }
    }

    override fun findEmote(name: String, channelId: String?): Emote? {
        if (channelId != null) {
            channelEmotesByChannelId[channelId]?.get(name)?.let {
                channelLastAccessedAtMillis[channelId] = System.currentTimeMillis()
                return decorate(it)
            }
        }
        return globalEmotesByName[name]?.let(::decorate)
    }

    override fun recordDimensions(emoteId: String, channelId: String?, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val prior = dimensionStore.get(emoteId)
        if (prior != null && prior.width == width && prior.height == height) return
        dimensionStore.record(emoteId, width, height)

        scope.launch { patchDiskAspectRatio(emoteId, channelId, width, height) }
    }

    override fun clearCache(channelId: String?) {
        if (channelId == null) {
            globalEmotesByName.clear()
            channelEmotesByChannelId.clear()
            channelLoadedAtMillis.clear()
            channelLastAccessedAtMillis.clear()
            globalLoadedAtMillis = 0L
            return
        }

        channelEmotesByChannelId.remove(channelId)
        channelLoadedAtMillis.remove(channelId)
        channelLastAccessedAtMillis.remove(channelId)
    }

    private fun decorate(emote: Emote): Emote {
        if (emote.aspectRatio != null) return emote
        val learned = dimensionStore.get(emote.id) ?: return emote
        return emote.copy(aspectRatio = learned.aspectRatio)
    }

    private suspend fun loadGlobalWithDiskFallback() {
        val snapshot = diskCache.readGlobal()
        if (snapshot != null && globalLoadedAtMillis == 0L) {
            mutex.withLock {
                if (globalLoadedAtMillis == 0L) {
                    globalEmotesByName.clear()
                    globalEmotesByName.putAll(snapshot.emotes)
                    globalLoadedAtMillis = snapshot.savedAtEpochMillis
                }
            }
        }

        val needsRefresh = snapshot == null ||
            System.currentTimeMillis() - snapshot.savedAtEpochMillis >= GLOBAL_CACHE_TTL_MILLIS

        if (!needsRefresh) return

        val refresh = scope.launch { refreshGlobalFromNetwork() }
        if (snapshot == null) refresh.join()
    }

    private suspend fun refreshGlobalFromNetwork() = withContext(Dispatchers.IO) {
        val loaded = loadGlobalEmotes()
        if (loaded.isEmpty()) return@withContext
        mutex.withLock {
            globalEmotesByName.clear()
            globalEmotesByName.putAll(loaded)
            globalLoadedAtMillis = System.currentTimeMillis()
        }
        diskCache.writeGlobal(loaded)
    }

    private suspend fun loadChannelWithDiskFallback(channelId: String) {
        channelLastAccessedAtMillis[channelId] = System.currentTimeMillis()

        val snapshot = diskCache.readChannel(channelId)
        if (snapshot != null && !channelEmotesByChannelId.containsKey(channelId)) {
            mutex.withLock {
                if (!channelEmotesByChannelId.containsKey(channelId)) {
                    channelEmotesByChannelId[channelId] = HashMap(snapshot.emotes)
                    channelLoadedAtMillis[channelId] = snapshot.savedAtEpochMillis
                }
            }
        }

        val needsRefresh = snapshot == null ||
            System.currentTimeMillis() - snapshot.savedAtEpochMillis >= CHANNEL_CACHE_TTL_MILLIS

        if (!needsRefresh) return

        val refresh: Deferred<Map<String, Emote>> = mutex.withLock {
            inFlightLoads[channelId] ?: scope.async {
                runCatching { fetchChannelEmotes(channelId) }.getOrElse { emptyMap() }
            }.also { inFlightLoads[channelId] = it }
        }

        val refreshJob = scope.launch {
            val loaded = try {
                refresh.await()
            } finally {
                mutex.withLock { inFlightLoads.remove(channelId) }
            }
            if (loaded.isNotEmpty()) {
                mutex.withLock {
                    channelEmotesByChannelId[channelId] = HashMap(loaded)
                    channelLoadedAtMillis[channelId] = System.currentTimeMillis()
                }
                diskCache.writeChannel(channelId, loaded)
            }
        }

        if (snapshot == null) refreshJob.join()
    }

    private suspend fun patchDiskAspectRatio(
        emoteId: String,
        channelId: String?,
        width: Int,
        height: Int
    ) {
        val ratio = width.toFloat() / height.toFloat()

        val (owningChannelId, emote) = locateEmote(emoteId, channelId) ?: return
        if (emote.aspectRatio == ratio) return
        val updated = emote.copy(aspectRatio = ratio)

        mutex.withLock {
            if (owningChannelId == null) {
                globalEmotesByName[emote.name] = updated
            } else {
                channelEmotesByChannelId[owningChannelId]?.put(emote.name, updated)
            }
        }
        diskCache.patchEmote(owningChannelId, updated)
    }

    private fun locateEmote(emoteId: String, hintedChannelId: String?): Pair<String?, Emote>? {
        if (hintedChannelId != null) {
            channelEmotesByChannelId[hintedChannelId]?.values
                ?.firstOrNull { it.id == emoteId }
                ?.let { return hintedChannelId to it }
        }
        globalEmotesByName.values.firstOrNull { it.id == emoteId }
            ?.let { return null to it }
        for ((cid, map) in channelEmotesByChannelId) {
            map.values.firstOrNull { it.id == emoteId }?.let { return cid to it }
        }
        return null
    }

    private suspend fun idleEvictionLoop() {
        while (true) {
            delay(IDLE_EVICTION_TICK_MILLIS)
            val cutoff = System.currentTimeMillis() - CHANNEL_IDLE_EVICT_MILLIS
            val victims = mutex.withLock {
                channelLastAccessedAtMillis
                    .filterValues { it < cutoff }
                    .keys
                    .toList()
            }
            if (victims.isEmpty()) continue
            mutex.withLock {
                for (channelId in victims) {
                    channelEmotesByChannelId.remove(channelId)
                    channelLoadedAtMillis.remove(channelId)
                    channelLastAccessedAtMillis.remove(channelId)
                }
            }
        }
    }

    private fun mergeWithPrecedence(
        sevenTv: List<Emote>,
        ffz: List<Emote>,
        bttv: List<Emote>
    ): HashMap<String, Emote> {
        val merged = HashMap<String, Emote>(sevenTv.size + ffz.size + bttv.size)
        for (emote in sevenTv) merged[emote.name] = emote
        for (emote in ffz) merged[emote.name] = emote
        for (emote in bttv) merged[emote.name] = emote
        return merged
    }

    private suspend fun loadGlobalEmotes(): HashMap<String, Emote> = coroutineScope {
        val globalSevenTv: Deferred<List<Emote>> = async {
            runCatching { sevenTvApi.getGlobalEmoteSet().emotes.map { it.toDomain() } }
                .getOrElse { emptyList() }
        }
        val globalFfz: Deferred<List<Emote>> = async {
            runCatching { ffzApi.getGlobalEmotes().map { it.toDomain() } }
                .getOrElse { emptyList() }
        }
        val globalBttv: Deferred<List<Emote>> = async {
            runCatching { bttvApi.getGlobalEmotes().map { it.toDomain() } }
                .getOrElse { emptyList() }
        }

        mergeWithPrecedence(
            sevenTv = globalSevenTv.await(),
            ffz = globalFfz.await(),
            bttv = globalBttv.await()
        )
    }

    private suspend fun fetchChannelEmotes(channelId: String): Map<String, Emote> = coroutineScope {
        val channelSevenTv: Deferred<List<Emote>> = async {
            runCatching { sevenTvApi.getChannelEmotes(channelId).map { it.toDomain() } }
                .getOrElse { emptyList() }
        }
        val channelFfz: Deferred<List<Emote>> = async {
            runCatching { ffzApi.getChannelEmotes(channelId).map { it.toDomain() } }
                .getOrElse { emptyList() }
        }
        val channelBttv: Deferred<List<Emote>> = async {
            runCatching { bttvApi.getChannelEmotes(channelId).map { it.toDomain() } }
                .getOrElse { emptyList() }
        }

        mergeWithPrecedence(
            sevenTv = channelSevenTv.await(),
            ffz = channelFfz.await(),
            bttv = channelBttv.await()
        )
    }

    companion object {
        private const val GLOBAL_CACHE_TTL_MILLIS = 6L * 60L * 60L * 1000L
        private const val CHANNEL_CACHE_TTL_MILLIS = 30L * 60L * 1000L
        private const val CHANNEL_IDLE_EVICT_MILLIS = 15L * 60L * 1000L
        private const val IDLE_EVICTION_TICK_MILLIS = 60L * 1000L
    }
}
