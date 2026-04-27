package com.example.chatterinomobile.data.local

import com.example.chatterinomobile.data.model.Badge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class BadgeDiskCache(private val root: DiskCacheRoot) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val fileLocks = HashMap<String, Mutex>()
    private val fileLocksGuard = Mutex()

    suspend fun readGlobalTwitch(): Snapshot<Map<String, Badge>>? = read(globalTwitchFile())

    suspend fun writeGlobalTwitch(badges: Map<String, Badge>) =
        write(globalTwitchFile(), badges)

    suspend fun readSevenTvByUser(): Snapshot<Map<String, List<Badge>>>? =
        readUserMap(sevenTvByUserFile())

    suspend fun writeSevenTvByUser(byUser: Map<String, List<Badge>>) =
        writeUserMap(sevenTvByUserFile(), byUser)

    suspend fun readChannel(channelId: String): Snapshot<Map<String, Badge>>? =
        read(channelFile(channelId))

    suspend fun writeChannel(channelId: String, badges: Map<String, Badge>) =
        write(channelFile(channelId), badges)

    suspend fun deleteChannel(channelId: String) = withContext(Dispatchers.IO) {
        lockFor(channelFile(channelId).absolutePath).withLock {
            channelFile(channelId).delete()
        }
        Unit
    }

    private suspend fun read(file: File): Snapshot<Map<String, Badge>>? =
        withContext(Dispatchers.IO) {
            lockFor(file.absolutePath).withLock {
                if (!file.exists()) null
                else runCatching {
                    json.decodeFromString(BadgeMapSnapshotDto.serializer(), file.readText())
                }.map { Snapshot(it.savedAtEpochMillis, it.entries) }.getOrNull()
            }
        }

    private suspend fun write(file: File, badges: Map<String, Badge>) =
        withContext(Dispatchers.IO) {
            lockFor(file.absolutePath).withLock {
                writeAtomic(
                    file,
                    json.encodeToString(
                        BadgeMapSnapshotDto.serializer(),
                        BadgeMapSnapshotDto(System.currentTimeMillis(), badges)
                    )
                )
            }
        }

    private suspend fun readUserMap(file: File): Snapshot<Map<String, List<Badge>>>? =
        withContext(Dispatchers.IO) {
            lockFor(file.absolutePath).withLock {
                if (!file.exists()) null
                else runCatching {
                    json.decodeFromString(UserBadgesSnapshotDto.serializer(), file.readText())
                }.map { Snapshot(it.savedAtEpochMillis, it.entries) }.getOrNull()
            }
        }

    private suspend fun writeUserMap(file: File, byUser: Map<String, List<Badge>>) =
        withContext(Dispatchers.IO) {
            lockFor(file.absolutePath).withLock {
                writeAtomic(
                    file,
                    json.encodeToString(
                        UserBadgesSnapshotDto.serializer(),
                        UserBadgesSnapshotDto(System.currentTimeMillis(), byUser)
                    )
                )
            }
        }

    private fun writeAtomic(file: File, payload: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(payload)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    private suspend fun lockFor(path: String): Mutex = fileLocksGuard.withLock {
        fileLocks.getOrPut(path) { Mutex() }
    }

    private fun globalTwitchFile(): File = File(root.badgeDir(), "global_twitch.json")
    private fun sevenTvByUserFile(): File = File(root.badgeDir(), "seventv_users.json")
    private fun channelFile(channelId: String): File =
        File(root.badgeDir(), "channel_${sanitize(channelId)}.json")

    private fun sanitize(id: String): String = id.filter { it.isLetterOrDigit() || it == '_' }

    data class Snapshot<T>(val savedAtEpochMillis: Long, val entries: T)

    @Serializable
    private data class BadgeMapSnapshotDto(
        val savedAtEpochMillis: Long,
        val entries: Map<String, Badge>
    )

    @Serializable
    private data class UserBadgesSnapshotDto(
        val savedAtEpochMillis: Long,
        val entries: Map<String, List<Badge>>
    )
}
