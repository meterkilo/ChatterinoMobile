package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Emote(
    val id: String,
    val name: String,
    val urls: EmoteUrls,
    val isAnimated: Boolean,
    val isZeroWidth: Boolean = false,
    val provider: EmoteProvider,
    val aspectRatio: Float? = null
)

@Serializable
data class EmoteUrls(
    val x1: String,
    val x2: String,
    val x3: String,
    val x4: String
) {
    fun forScale(scale: Int): String = when (scale) {
        1 -> x1
        2 -> x2
        3 -> x3
        else -> x4
    }
}

@Serializable
enum class EmoteProvider {
    TWITCH, BTTV, FFZ, SEVENTV
}
