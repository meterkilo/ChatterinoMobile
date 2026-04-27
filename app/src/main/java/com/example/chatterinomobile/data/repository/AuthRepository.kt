package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.TwitchImplicitAuthResult

interface AuthRepository {

    suspend fun getAccessToken(): String?

    suspend fun getUserId(): String?

    suspend fun getLogin(): String?

    fun getClientId(): String

    fun buildAuthorizeUrl(scopes: List<String> = DEFAULT_TWITCH_SCOPES): String?

    suspend fun completeImplicitFlow(redirectUrl: String): TwitchImplicitAuthResult

    suspend fun clearSession()

    companion object {
        const val REDIRECT_URI = "http://localhost"

        val DEFAULT_TWITCH_SCOPES = listOf(
            "chat:read",
            "chat:edit",
            "user:read:follows"
        )
    }
}
