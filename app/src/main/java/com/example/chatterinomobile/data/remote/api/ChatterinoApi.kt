package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.ChatterinoBadgesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ChatterinoApi(private val httpClient: HttpClient) {

    suspend fun getBadges(): ChatterinoBadgesResponseDto =
        httpClient.get("$BASE_URL/badges").body()

    companion object {
        private const val BASE_URL = "https://api.chatterino.com"
    }
}
