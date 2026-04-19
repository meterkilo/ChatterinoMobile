package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root response for FFZ global emotes endpoint.
 * FFZ returns { "default_sets": [...], "sets": { "3": {...}, "4": {...} } }
 */
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
data class FfzEmoteDto(
    val id: Int,
    val name: String,
    val animated: Map<String, String>? = null,  // null for static emotes
    val urls: Map<String, String>                // keys: "1", "2", "4"
)