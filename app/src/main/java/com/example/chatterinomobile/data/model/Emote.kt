package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

/**
 * A chat emote from any provider (Twitch, BTTV, FFZ, 7TV).
 *
 * Performance notes:
 * - [aspectRatio] is `width / height` and is used by the renderer to reserve
 *   exact space in a chat bubble before the image has finished decoding. This
 *   eliminates layout jumps on fast chats — the single biggest readability win
 *   in a Chatterino-style client. 7TV gives us dimensions up front; BTTV/FFZ
 *   don't, so [aspectRatio] is learned on first decode via
 *   [com.example.chatterinomobile.data.repository.EmoteRepository.recordDimensions]
 *   and persisted to [com.example.chatterinomobile.data.local.EmoteDimensionStore].
 * - [isZeroWidth] matters for 7TV stacking emotes (e.g. RainTime over a base emote).
 *
 * `@Serializable` so the whole metadata blob can be written to disk as JSON for
 * instant cold-start restore — see
 * [com.example.chatterinomobile.data.local.EmoteDiskCache].
 */
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


/**
 * The four standard emote resolutions. Wrapped in a type so the URL-picking
 * logic lives with the data instead of being scattered across the UI.
 */
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
