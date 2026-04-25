package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

/**
 * A user badge (subscriber, mod, VIP, 7TV dev, etc.)
 * [description] doubles as a11y contentDescription and tooltip text.
 *
 * `@Serializable` so [com.example.chatterinomobile.data.local.BadgeDiskCache]
 * can persist the lookup tables between launches.
 */
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
