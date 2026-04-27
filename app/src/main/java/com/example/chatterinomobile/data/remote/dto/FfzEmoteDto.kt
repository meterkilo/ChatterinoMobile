package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FfzGlobalResponseDto(
    @SerialName("default_sets") val defaultSets: List<Int>,
    val sets: Map<String, FfzEmoteSetDto>
)

@Serializable
data class FfzEmoteSetDto(
    val id: Int,
    val emoticons: List<FfzEmoteDto>
)

@Serializable
data class FfzRoomResponseDto(
    val room: FfzRoomDto,
    val sets: Map<String, FfzEmoteSetDto>
)

@Serializable
data class FfzRoomDto(
    val set: Int? = null
)

@Serializable
data class FfzEmoteDto(
    val id: Int,
    val name: String,
    val animated: Map<String, String>? = null,
    val urls: Map<String, String>
)
