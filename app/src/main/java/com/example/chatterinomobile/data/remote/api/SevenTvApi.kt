package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.SevenTvEmoteSetDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class SevenTvApi(private val httpClient: HttpClient) {

    suspend fun getGlobalEmoteSet(): SevenTvEmoteSetDto {
        return httpClient.get("$BASE_URL/emote-sets/global").body()
    }

    companion object {
        private const val BASE_URL = "https://7tv.io/v3"
    }
}