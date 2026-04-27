package com.example.chatterinomobile.data.local

import com.example.chatterinomobile.data.model.Emote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class EmoteDiskCache(private val root: DiskCacheRoot) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val fileLocks = HashMap<String, Mutex>()
    private val fileLocksGuard = Mutex()

    suspend fun readGlobal(): Snapshot? = read(globalFile())

    suspend fun writeGlobal(emotes: Map<String, Emote>) = write(globalFile(), emotes)

    suspend fun readChannel(channelId: String): Snapshot? = read(channelFile(channelId))

    suspend fun writeChannel(channelId: String, emotes: Map<String, Emote>) =
        write(channelFile(channelId), emotes)

    suspend fun deleteChannel(channelId: String) = withContext(Dispatchers.IO) {
        lockFor(channelFile(channelId).absolutePath).withLock {
            channelFile(channelId).delete()
        }
        Unit
    }

    suspend fun patchEmote(channelId: String?, updated: Emote) = withContext(Dispatchers.IO) {
        val file = if (channelId == null) globalFile() else channelFile(channelId)
        lockFor(file.absolutePath).withLock {
            val current = readLocked(file) ?: return@withContext
            val merged = HashMap(current.emotes)
            merged[updated.name] = updated
            writeLocked(file, Snapshot(current.savedAtEpochMillis, merged))
        }
    }

    private suspend fun read(file: File): Snapshot? = withContext(Dispatchers.IO) {
        lockFor(file.absolutePath).withLock { readLocked(file) }
    }

    private fun readLocked(file: File): Snapshot? {
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(SnapshotDto.serializer(), file.readText()) }
            .map { Snapshot(it.savedAtEpochMillis, it.emotes) }
            .getOrNull()
    }

    private suspend fun write(file: File, emotes: Map<String, Emote>) = withContext(Dispatchers.IO) {
        lockFor(file.absolutePath).withLock {
            writeLocked(file, Snapshot(System.currentTimeMillis(), emotes))
        }
    }

    private fun writeLocked(file: File, snapshot: Snapshot) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        val payload = json.encodeToString(
            SnapshotDto.serializer(),
            SnapshotDto(snapshot.savedAtEpochMillis, snapshot.emotes)
        )
        tmp.writeText(payload)
        if (!tmp.renameTo(file)) {

            file.delete()
            tmp.renameTo(file)
        }
    }

    private suspend fun lockFor(path: String): Mutex = fileLocksGuard.withLock {
        fileLocks.getOrPut(path) { Mutex() }
    }

    private fun globalFile(): File = File(root.emoteDir(), "global.json")

    private fun channelFile(channelId: String): File =
        File(root.emoteDir(), "channel_${sanitize(channelId)}.json")

    private fun sanitize(id: String): String = id.filter { it.isLetterOrDigit() || it == '_' }

    data class Snapshot(val savedAtEpochMillis: Long, val emotes: Map<String, Emote>)

    @Serializable
    private data class SnapshotDto(
        val savedAtEpochMillis: Long,
        val emotes: Map<String, Emote>
    )
}
