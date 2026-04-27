package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.BadgeProvider
import com.example.chatterinomobile.data.model.ColorStop
import com.example.chatterinomobile.data.model.GradientFunction
import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.model.Shadow
import com.example.chatterinomobile.data.remote.dto.SevenTvBadgeDto
import com.example.chatterinomobile.data.remote.dto.SevenTvColorStopDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintDto
import com.example.chatterinomobile.data.remote.dto.SevenTvShadowDto

fun SevenTvPaintDto.toDomain(): Paint {
    val mappedShadows = shadows.map { it.toDomain() }

    return when (function.uppercase()) {
        "URL" -> Paint.Image(
            id = id,
            url = imageUrl.orEmpty(),
            shadows = mappedShadows
        )

        "LINEAR_GRADIENT" -> Paint.Gradient(
            id = id,
            function = GradientFunction.LINEAR,
            angle = angle,
            stops = stops.map { it.toDomain() },
            repeating = repeat,
            shadows = mappedShadows
        )

        "RADIAL_GRADIENT" -> Paint.Gradient(
            id = id,
            function = GradientFunction.RADIAL,
            angle = angle,
            stops = stops.map { it.toDomain() },
            repeating = repeat,
            shadows = mappedShadows
        )

        "CONIC_GRADIENT" -> Paint.Gradient(
            id = id,
            function = GradientFunction.CONIC,
            angle = angle,
            stops = stops.map { it.toDomain() },
            repeating = repeat,
            shadows = mappedShadows
        )

        else -> Paint.Solid(
            id = id,
            color = color ?: 0xFFFFFFFFL,
            shadows = mappedShadows
        )
    }
}

fun SevenTvColorStopDto.toDomain(): ColorStop = ColorStop(at = at, color = color)

fun SevenTvShadowDto.toDomain(): Shadow = Shadow(
    xOffset = xOffset,
    yOffset = yOffset,
    radius = radius,
    color = color
)

fun SevenTvBadgeDto.toDomain(): Badge {
    val url = pickSize("3") ?: pickSize("2") ?: pickSize("1") ?: ""
    return Badge(
        id = id,
        imageURL = url,
        description = tooltip.ifBlank { name },
        provider = BadgeProvider.SEVENTV
    )
}

private fun SevenTvBadgeDto.pickSize(size: String): String? =
    urls.firstOrNull { it.size >= 2 && it[0] == size }?.get(1)
