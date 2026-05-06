package com.example.chatterinomobile.data.local

import android.content.Context

class PinnedChannelsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun read(): List<String> {
        val raw = prefs.getString(KEY_LOGINS, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').filter { it.isNotBlank() }
    }

    fun write(logins: List<String>) {
        prefs.edit()
            .putString(KEY_LOGINS, logins.joinToString(","))
            .apply()
    }

    companion object {
        private const val FILE_NAME = "pinned_channels"
        private const val KEY_LOGINS = "logins"
    }
}
