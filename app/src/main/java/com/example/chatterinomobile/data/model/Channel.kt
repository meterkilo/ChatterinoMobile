package com.example.chatterinomobile.data.model

data class Channel(
    val id: String,
    val login: String,
    val displayName: String,
    val isLive: Boolean = false,
    val viewerCount: Int = 0,
    val gameName: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null
)
