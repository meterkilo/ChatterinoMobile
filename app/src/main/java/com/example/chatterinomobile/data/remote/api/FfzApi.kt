package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.FfzEmoteDto
import com.example.chatterinomobile.data.remote.dto.FfzGlobalResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class FfzApi(private val httpClient: HttpClient) {

    suspend fun getGlobalEmotes(): List<FfzEmoteDto> {
        val response: FfzGlobalResponseDto = httpClient.get("$BASE_URL/set/global").body()
        // Flatten: take only sets that are in default_sets, then concat their emotes.
        return response.defaultSets
            .mapNotNull { setId -> response.sets[setId.toString()] }
            .flatMap { it.emoticons }
    }

    companion object {
        private const val BASE_URL = "https://api.frankerfacez.com/v1"
    }
}