package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.FfzEmoteDto
import com.example.chatterinomobile.data.remote.dto.FfzBadgesResponseDto
import com.example.chatterinomobile.data.remote.dto.FfzGlobalResponseDto
import com.example.chatterinomobile.data.remote.dto.FfzRoomResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class FfzApi(private val httpClient: HttpClient) {

    suspend fun getGlobalEmotes(): List<FfzEmoteDto> {
        val response: FfzGlobalResponseDto = httpClient.get("$BASE_URL/set/global").body()

        return response.defaultSets
            .mapNotNull { setId -> response.sets[setId.toString()] }
            .flatMap { it.emoticons }
    }

    suspend fun getChannelEmotes(channelId: String): List<FfzEmoteDto> {
        val response: FfzRoomResponseDto = httpClient.get("$BASE_URL/room/id/$channelId").body()
        val setId = response.room.set ?: return emptyList()
        return response.sets[setId.toString()]?.emoticons.orEmpty()
    }

    suspend fun getBadges(): FfzBadgesResponseDto =
        httpClient.get("$BASE_URL/badges/ids").body()

    suspend fun getRoom(channelId: String): FfzRoomResponseDto =
        httpClient.get("$BASE_URL/room/id/$channelId").body()

    companion object {
        private const val BASE_URL = "https://api.frankerfacez.com/v1"
    }
}
