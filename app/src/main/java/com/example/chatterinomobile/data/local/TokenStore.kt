package com.example.chatterinomobile.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {

    private val appContext = context.applicationContext

    fun read(): StoredTwitchSession? {
        val sharedPreferences = sharedPreferences()
        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return StoredTwitchSession(
            accessToken = accessToken,
            refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null),
            userId = sharedPreferences.getString(KEY_USER_ID, null),
            login = sharedPreferences.getString(KEY_LOGIN, null),
            expiresAtEpochMillis = sharedPreferences.getLong(KEY_EXPIRES_AT, 0L),
            scopes = sharedPreferences.getStringSet(KEY_SCOPES, emptySet()).orEmpty().toList(),
            lastValidatedAtEpochMillis = sharedPreferences.getLong(KEY_LAST_VALIDATED_AT, 0L)
        )
    }

    fun write(session: StoredTwitchSession) {
        val sharedPreferences = sharedPreferences()
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_LOGIN, session.login)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis)
            .putStringSet(KEY_SCOPES, session.scopes.toSet())
            .putLong(KEY_LAST_VALIDATED_AT, session.lastValidatedAtEpochMillis)
            .apply()
    }

    fun clear() {
        sharedPreferences().edit().clear().apply()
    }

    private fun sharedPreferences() =
        runCatching { createSharedPreferences() }
            .recoverCatching {
                appContext.deleteSharedPreferences(FILE_NAME)
                createSharedPreferences()
            }
            .getOrThrow()

    private fun createSharedPreferences() = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    data class StoredTwitchSession(
        val accessToken: String,
        val refreshToken: String?,
        val userId: String?,
        val login: String?,
        val expiresAtEpochMillis: Long,
        val scopes: List<String>,
        val lastValidatedAtEpochMillis: Long
    )

    companion object {
        private const val FILE_NAME = "twitch_oauth_store"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOGIN = "login"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_millis"
        private const val KEY_SCOPES = "scopes"
        private const val KEY_LAST_VALIDATED_AT = "last_validated_at_epoch_millis"
    }
}
