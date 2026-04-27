package com.example.chatterinomobile.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class EmoteDimensionStore(
    private val root: DiskCacheRoot,
    scopeOverride: CoroutineScope? = null
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cache = HashMap<String, Dimensions>()
    private val writeMutex = Mutex()
    private val scope = scopeOverride ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var loaded = false
    @Volatile private var pendingWrite: Job? = null
    @Volatile private var dirty = false

    suspend fun ensureLoaded() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            val file = root.dimensionFile()
            val fromDisk = if (!file.exists()) emptyMap()
            else runCatching {
                json.decodeFromString(StoreDto.serializer(), file.readText()).entries
            }.getOrElse { emptyMap() }
            writeMutex.withLock {
                if (!loaded) {
                    cache.putAll(fromDisk)
                    loaded = true
                }
            }
        }
    }

    fun get(emoteId: String): Dimensions? = cache[emoteId]

    fun record(emoteId: String, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val existing = cache[emoteId]
        if (existing != null && existing.width == width && existing.height == height) return
        cache[emoteId] = Dimensions(width, height)
        dirty = true
        scheduleFlush()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        pendingWrite?.cancel()
        writeMutex.withLock {
            cache.clear()
            dirty = false
            root.dimensionFile().delete()
        }
    }

    suspend fun flushNow() {
        pendingWrite?.cancel()
        writeIfDirty()
    }

    private fun scheduleFlush() {
        val existing = pendingWrite
        if (existing != null && existing.isActive) return
        pendingWrite = scope.launch {
            delay(DEBOUNCE_MILLIS)
            writeIfDirty()
        }
    }

    private suspend fun writeIfDirty() {
        if (!dirty) return
        writeMutex.withLock {
            if (!dirty) return@withLock
            val snapshot = HashMap(cache)
            dirty = false
            val file = root.dimensionFile()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(StoreDto.serializer(), StoreDto(snapshot)))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    @Serializable
    data class Dimensions(val width: Int, val height: Int) {
        val aspectRatio: Float
            get() = if (height == 0) 1f else width.toFloat() / height.toFloat()
    }

    @Serializable
    private data class StoreDto(val entries: Map<String, Dimensions>)

    companion object {
        private const val DEBOUNCE_MILLIS = 1_500L
    }
}
