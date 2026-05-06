package com.example.chatterinomobile.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class FollowListCache(private val root: DiskCacheRoot) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun read(userId: String): Snapshot? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(userId)
            if (!file.exists()) return@withLock null
            runCatching {
                val dto = json.decodeFromString(SnapshotDto.serializer(), file.readText())
                Snapshot(dto.savedAtEpochMillis, dto.logins)
            }.getOrNull()
        }
    }

    suspend fun write(userId: String, logins: List<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(userId)
            val tmp = File(file.parentFile, file.name + ".tmp")
            val payload = json.encodeToString(
                SnapshotDto.serializer(),
                SnapshotDto(System.currentTimeMillis(), logins)
            )
            tmp.writeText(payload)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    suspend fun clear(userId: String) = withContext(Dispatchers.IO) {
        mutex.withLock { fileFor(userId).delete() }
    }

    private fun fileFor(userId: String): File =
        File(root.followsDir(), "follows_${userId.filter { it.isLetterOrDigit() }}.json")

    data class Snapshot(val savedAtEpochMillis: Long, val logins: List<String>)

    @Serializable
    private data class SnapshotDto(val savedAtEpochMillis: Long, val logins: List<String>)
}
