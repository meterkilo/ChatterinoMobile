package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevenTvEmoteSetDto(
    val id: String,
    val name: String,
    @SerialName("emote_count") val emoteCount: Int,
    val emotes: List<SevenTvActiveEmoteDto>
)

@Serializable
data class SevenTvActiveEmoteDto(
    val id: String,
    val name: String,
    val flags: Int,
    val data: SevenTvEmoteDataDto
)

@Serializable
data class SevenTvEmoteDataDto(
    val id: String,
    val name: String,
    val animated: Boolean,
    val host: SevenTvHostDto
)

@Serializable
data class SevenTvHostDto(
    val url: String,
    val files: List<SevenTvFileDto>
)

@Serializable
data class SevenTvFileDto(
    val name: String,
    val width: Int,
    val height: Int,
    val format: String
)