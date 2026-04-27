package com.example.chatterinomobile.data.model

sealed interface TwitchImplicitAuthResult {

    data class Authorized(
        val userId: String?,
        val login: String?,
        val accessToken: String,
        val scopes: List<String>,
        val expiresAtEpochMillis: Long
    ) : TwitchImplicitAuthResult

    data object Denied : TwitchImplicitAuthResult

    data class Failed(val message: String) : TwitchImplicitAuthResult
}
