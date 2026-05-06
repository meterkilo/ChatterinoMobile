package com.example.chatterinomobile.data.local

import com.example.chatterinomobile.data.model.Category
import com.example.chatterinomobile.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class DiscoverySnapshotCache(private val root: DiskCacheRoot) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun read(userKey: String): Snapshot? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(userKey)
            if (!file.exists()) return@withLock null
            runCatching {
                val dto = json.decodeFromString(SnapshotDto.serializer(), file.readText())
                Snapshot(
                    savedAtEpochMillis = dto.savedAtEpochMillis,
                    followedLive = dto.followedLive.map { it.toModel() },
                    topLiveStreams = dto.topLiveStreams.map { it.toModel() },
                    topCategories = dto.topCategories.map { it.toModel() }
                )
            }.getOrNull()
        }
    }

    suspend fun write(
        userKey: String,
        followedLive: List<Channel>,
        topLiveStreams: List<Channel>,
        topCategories: List<Category>
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(userKey)
            val tmp = File(file.parentFile, file.name + ".tmp")
            val payload = json.encodeToString(
                SnapshotDto.serializer(),
                SnapshotDto(
                    savedAtEpochMillis = System.currentTimeMillis(),
                    followedLive = followedLive.map { ChannelDto.from(it) },
                    topLiveStreams = topLiveStreams.map { ChannelDto.from(it) },
                    topCategories = topCategories.map { CategoryDto.from(it) }
                )
            )
            tmp.writeText(payload)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    private fun fileFor(userKey: String): File =
        File(root.discoveryDir(), "snapshot_${userKey.filter { it.isLetterOrDigit() }}.json")

    data class Snapshot(
        val savedAtEpochMillis: Long,
        val followedLive: List<Channel>,
        val topLiveStreams: List<Channel>,
        val topCategories: List<Category>
    )

    @Serializable
    private data class SnapshotDto(
        val savedAtEpochMillis: Long,
        val followedLive: List<ChannelDto>,
        val topLiveStreams: List<ChannelDto>,
        val topCategories: List<CategoryDto>
    )

    @Serializable
    private data class ChannelDto(
        val id: String,
        val login: String,
        val displayName: String,
        val isLive: Boolean = false,
        val viewerCount: Int = 0,
        val gameName: String? = null,
        val title: String? = null,
        val thumbnailUrl: String? = null,
        val profileImageUrl: String? = null
    ) {
        fun toModel() = Channel(
            id = id,
            login = login,
            displayName = displayName,
            isLive = isLive,
            viewerCount = viewerCount,
            gameName = gameName,
            title = title,
            thumbnailUrl = thumbnailUrl,
            profileImageUrl = profileImageUrl
        )

        companion object {
            fun from(c: Channel) = ChannelDto(
                id = c.id,
                login = c.login,
                displayName = c.displayName,
                isLive = c.isLive,
                viewerCount = c.viewerCount,
                gameName = c.gameName,
                title = c.title,
                thumbnailUrl = c.thumbnailUrl,
                profileImageUrl = c.profileImageUrl
            )
        }
    }

    @Serializable
    private data class CategoryDto(
        val id: String,
        val name: String,
        val boxArtUrl: String? = null,
        val viewerCount: Int = 0
    ) {
        fun toModel() = Category(
            id = id,
            name = name,
            boxArtUrl = boxArtUrl,
            viewerCount = viewerCount
        )

        companion object {
            fun from(c: Category) = CategoryDto(
                id = c.id,
                name = c.name,
                boxArtUrl = c.boxArtUrl,
                viewerCount = c.viewerCount
            )
        }
    }
}
