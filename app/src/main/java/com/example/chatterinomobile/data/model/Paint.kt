package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

/**
 * A 7TV paint applied to a username. Sealed class so the renderer can
 * exhaustively handle each variant via `when`.
 *
 * `@Serializable` so persisted chat history can faithfully reproduce the
 * paint that was active *at the time the message was received*, even if the
 * user's current paint has since changed. See
 * [com.example.chatterinomobile.data.local.MessageHistoryStore].
 */

@Serializable
sealed class Paint {
    abstract val id: String
    abstract val shadows: List<Shadow>

    /** Flat color fill */
    @Serializable
    data class Solid(
        override val id: String,
        val color : Long,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()

    /** Multi-stop gradient (linear / radial / conic). */

    @Serializable
    data class Gradient(
        override val id: String,
        val function: GradientFunction,
        val angle: Int,
        val stops: List<ColorStop>,
        val repeating: Boolean = false,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()


    /** Image-based paint (animated WEBP or static). */
    @Serializable
    data class Image(
        override val id: String,
        val url: String,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()
}

@Serializable
enum class GradientFunction { LINEAR, RADIAL, CONIC }

/**
 * One stop in a gradient.
 * @param at position from 0.0 (start) to 1.0 (end)
 * @param color ARGB color as Long
 */
@Serializable
data class ColorStop(
    val at: Float,
    val color: Long
)

/**
 * A text shadow. Multiple shadows can stack on the same paint.
 */
@Serializable
data class Shadow(
    val xOffset: Float,
    val yOffset: Float,
    val radius: Float,
    val color: Long
)
