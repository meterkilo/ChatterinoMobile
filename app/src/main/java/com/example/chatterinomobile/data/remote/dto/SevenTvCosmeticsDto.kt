package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevenTvCosmeticsResponseDto(
    val paints: List<SevenTvPaintDto> = emptyList(),
    val badges: List<SevenTvBadgeDto> = emptyList()
)

@Serializable
data class SevenTvPaintDto(
    val id: String,
    val name: String,

    val users: List<String> = emptyList(),

    val function: String,

    val color: Long? = null,
    val stops: List<SevenTvColorStopDto> = emptyList(),
    val repeat: Boolean = false,
    val angle: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    val shadows: List<SevenTvShadowDto> = emptyList()
)

@Serializable
data class SevenTvColorStopDto(
    val at: Float,
    val color: Long
)

@Serializable
data class SevenTvShadowDto(
    @SerialName("x_offset") val xOffset: Float,
    @SerialName("y_offset") val yOffset: Float,
    val radius: Float,
    val color: Long
)

@Serializable
data class SevenTvBadgeDto(
    val id: String,
    val name: String,
    val tooltip: String,
    val tag: String = "",

    val users: List<String> = emptyList(),

    val urls: List<List<String>> = emptyList()
)
