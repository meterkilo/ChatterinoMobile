package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.TwitchImplicitAuthResult

/**
 * Stub [AuthRepository] that always returns "no user logged in".
 *
 * All downstream callers must tolerate this (the IRC client falls back to a
 * `justinfan*` anonymous login, Helix endpoints that require auth return empty).
 */
class AnonymousAuthRepository(
    private val clientId: String = ""
) : AuthRepository {

    override suspend fun getAccessToken(): String? = null

    override suspend fun getUserId(): String? = null

    override suspend fun getLogin(): String? = null

    override fun getClientId(): String = clientId

    override fun buildAuthorizeUrl(scopes: List<String>): String? = null

    override suspend fun completeImplicitFlow(redirectUrl: String): TwitchImplicitAuthResult =
        TwitchImplicitAuthResult.Failed("Twitch OAuth is not configured")

    override suspend fun clearSession() = Unit
}
