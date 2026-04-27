package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.SevenTvCosmeticsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class SevenTvCosmeticsApi(private val httpClient: HttpClient) {

    suspend fun getCosmetics(userIdentifier: String = "twitch_id"): SevenTvCosmeticsResponseDto {
        return httpClient.get("$BASE_URL/cosmetics") {
            parameter("user_identifier", userIdentifier)
        }.body()
    }

    companion object {
        private const val BASE_URL = "https://api.7tv.app/v2"
    }
}
