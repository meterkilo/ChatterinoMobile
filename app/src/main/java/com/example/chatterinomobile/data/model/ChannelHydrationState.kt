package com.example.chatterinomobile.data.model

data class ChannelHydrationState(
    val channelLogin: String,
    val channelId: String? = null,
    val channel: Channel? = null,
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val errorMessage: String? = null
)
