package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Badge(
    val id: String,
    val imageURL: String,
    val description: String,
    val provider: BadgeProvider
)

@Serializable
enum class BadgeProvider{
    TWITCH, BTTV, FFZ, SEVENTV
}
