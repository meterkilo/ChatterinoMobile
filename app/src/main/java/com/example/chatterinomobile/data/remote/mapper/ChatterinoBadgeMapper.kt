package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.BadgeProvider
import com.example.chatterinomobile.data.remote.dto.ChatterinoBadgeDto

fun ChatterinoBadgeDto.toDomain(index: Int): Badge =
    Badge(
        id = "chatterino:$index:$tooltip",
        imageURL = image3.ifBlank { image2.ifBlank { image1 } },
        description = tooltip,
        provider = BadgeProvider.CHATTERINO
    )
