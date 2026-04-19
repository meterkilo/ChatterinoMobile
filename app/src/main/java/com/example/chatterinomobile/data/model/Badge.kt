package com.example.chatterinomobile.data.model

/**
 * A user badge (subscriber, mod, VIP, 7TV dev, etc.)
 * [description] doubles as a11y contentDescription and tooltip text.
 */
data class Badge(
    val id: String,
    val imageURL: String,
    val description: String,
    val provider: BadgeProvider
)

enum class BadgeProvider{
    TWITCH, BTTV, FFZ, SEVENTV
}

