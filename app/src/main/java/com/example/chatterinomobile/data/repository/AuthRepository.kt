package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.TwitchImplicitAuthResult

/**
 * Supplies a Twitch OAuth access token for endpoints/WebSockets that require one.
 *
 * The key design choice here is that "logged out" is not an exceptional state.
 * The rest of the app must keep functioning in anonymous read-only mode when:
 * - the user never signed in
 * - the configured Twitch client ID is blank
 * - a stored token is no longer valid and we intentionally clear it
 */
interface AuthRepository {

    suspend fun getAccessToken(): String?

    suspend fun getUserId(): String?

    suspend fun getLogin(): String?

    fun getClientId(): String

    /**
     * Builds the Twitch authorize URL for the implicit grant flow. Returns
     * null when the client ID isn't configured. The UI loads this URL in a
     * WebView and watches for redirects to [REDIRECT_URI].
     */
    fun buildAuthorizeUrl(scopes: List<String> = DEFAULT_TWITCH_SCOPES): String?

    /**
     * Called by the UI after the WebView captures a redirect back to
     * [REDIRECT_URI]. Parses the URL fragment for the access token, validates
     * it against Twitch, and persists the resulting session.
     */
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
