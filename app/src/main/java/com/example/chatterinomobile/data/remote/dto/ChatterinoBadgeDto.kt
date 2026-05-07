package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatterinoBadgesResponseDto(
    val badges: List<ChatterinoBadgeDto> = emptyList()
)

@Serializable
data class ChatterinoBadgeDto(
    val tooltip: String = "",
    val image1: String = "",
    val image2: String = "",
    val image3: String = "",
    val users: List<String> = emptyList()
)
