package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.BttvEmoteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class BttvApi(private val httpClient: HttpClient) {

    suspend fun getGlobalEmotes(): List<BttvEmoteDto> {
        return httpClient.get("$BASE_URL/cached/emotes/global").body()
    }

    companion object {
        private const val BASE_URL = "https://api.betterttv.net/3"
    }
}