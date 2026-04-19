package com.example.chatterinomobile.data.model
/**
 * A chat emote from any provider (Twitch, BTTV, FFZ, 7TV).
 *
 * Performance notes:
 * - [aspectRatio] is cached after first image load to prevent layout jumps.
 * - [isZeroWidth] matters for 7TV stacking emotes (e.g. RainTime over a base emote).
 */

data class Emote(
    val id: String,
    val name: String,
    val urls: EmoteUrls,
    val isAnimated: Boolean,
    val isZeroWidth: Boolean = false,
    val provider: EmoteProvider,
    val aspectRatio: Float? = null
)


/**
 * The four standard emote resolutions. Wrapped in a type so the URL-picking
 * logic lives with the data instead of being scattered across the UI.
 */
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

enum class EmoteProvider {
    TWITCH, BTTV, FFZ, SEVENTV
}

