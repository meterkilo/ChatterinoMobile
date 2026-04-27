package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Paint {
    abstract val id: String
    abstract val shadows: List<Shadow>

    @Serializable
    data class Solid(
        override val id: String,
        val color : Long,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()

    @Serializable
    data class Gradient(
        override val id: String,
        val function: GradientFunction,
        val angle: Int,
        val stops: List<ColorStop>,
        val repeating: Boolean = false,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()

    @Serializable
    data class Image(
        override val id: String,
        val url: String,
        override val shadows: List<Shadow> = emptyList()
    ) : Paint()
}

@Serializable
enum class GradientFunction { LINEAR, RADIAL, CONIC }

@Serializable
data class ColorStop(
    val at: Float,
    val color: Long
)

@Serializable
data class Shadow(
    val xOffset: Float,
    val yOffset: Float,
    val radius: Float,
    val color: Long
)
