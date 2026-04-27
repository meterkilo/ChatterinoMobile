package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.BadgeProvider
import com.example.chatterinomobile.data.remote.dto.HelixBadgeVersionDto

fun HelixBadgeVersionDto.toDomain(setId: String): Badge = Badge(
    id = "$setId/$id",
    imageURL = imageUrl4x,
    description = description.ifBlank { title },
    provider = BadgeProvider.TWITCH
)

fun twitchBadgeKey(setId: String, version: String): String = "$setId/$version"
