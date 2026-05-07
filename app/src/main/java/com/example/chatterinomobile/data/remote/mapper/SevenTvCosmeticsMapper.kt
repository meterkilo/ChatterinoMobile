package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.BadgeProvider
import com.example.chatterinomobile.data.model.ColorStop
import com.example.chatterinomobile.data.model.GradientFunction
import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.model.Shadow
import com.example.chatterinomobile.data.remote.dto.SevenTvActiveBadgeDto
import com.example.chatterinomobile.data.remote.dto.SevenTvActivePaintDto
import com.example.chatterinomobile.data.remote.dto.SevenTvColorDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintGradientStopDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintImageDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintLayerDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintShadowDto

fun SevenTvActivePaintDto.toDomain(): Paint? {
    val shadows = data.shadows.map { it.toShadow() }
    val firstRenderable = data.layers.firstOrNull { it.opacity > 0f } ?: data.layers.firstOrNull()
    val ty = firstRenderable?.ty ?: return null

    return when (ty.typename) {
        "PaintLayerTypeSingleColor" -> Paint.Solid(
            id = id,
            color = ty.color.toArgbLong(),
            shadows = shadows
        )

        "PaintLayerTypeLinearGradient" -> Paint.Gradient(
            id = id,
            function = GradientFunction.LINEAR,
            angle = ty.angle,
            stops = ty.stops.map { it.toColorStop() },
            repeating = ty.repeating,
            shadows = shadows
        )

        "PaintLayerTypeRadialGradient" -> Paint.Gradient(
            id = id,
            function = GradientFunction.RADIAL,
            angle = 0,
            stops = ty.stops.map { it.toColorStop() },
            repeating = ty.repeating,
            shadows = shadows
        )

        "PaintLayerTypeImage" -> {
            val best = ty.images.pickBestPaintImage() ?: return null
            val ratio = if (best.height > 0) best.width.toFloat() / best.height.toFloat() else 4f
            Paint.Image(
                id = id,
                url = best.url,
                aspectRatio = ratio,
                animated = best.frameCount > 1,
                shadows = shadows
            )
        }

        else -> null
    }
}

fun SevenTvActiveBadgeDto.toDomain(): Badge {
    val best = images.pickBestBadgeImage()
    return Badge(
        id = id,
        imageURL = best?.url.orEmpty(),
        description = description?.takeIf { it.isNotBlank() } ?: name,
        provider = BadgeProvider.SEVENTV
    )
}

private fun List<SevenTvPaintImageDto>.pickBestPaintImage(): SevenTvPaintImageDto? {
    if (isEmpty()) return null
    val animated = filter { it.frameCount > 1 }
    val pool = if (animated.isNotEmpty()) animated else this
    val supported = pool.filter { it.mime.isSupportedMime() }
    val candidate = if (supported.isNotEmpty()) supported else pool
    return candidate.maxByOrNull { it.scale * 100 + it.qualityRank() }
}

private fun List<SevenTvPaintImageDto>.pickBestBadgeImage(): SevenTvPaintImageDto? {
    if (isEmpty()) return null
    val supported = filter { it.mime.isSupportedMime() }
    val pool = if (supported.isNotEmpty()) supported else this
    return pool.maxByOrNull { it.scale * 100 + it.qualityRank() }
}

private fun SevenTvPaintImageDto.qualityRank(): Int = when {
    mime.equals("image/webp", true) -> 30
    mime.equals("image/png", true) -> 20
    mime.equals("image/avif", true) -> 10
    mime.equals("image/gif", true) -> 5
    else -> 0
}

private fun String.isSupportedMime(): Boolean {
    val lower = lowercase()
    return lower == "image/webp" || lower == "image/png" || lower == "image/gif"
}

private fun SevenTvColorDto?.toArgbLong(): Long {
    if (this == null) return 0xFFFFFFFFL
    val a = (a and 0xFF).toLong()
    val r = (r and 0xFF).toLong()
    val g = (g and 0xFF).toLong()
    val b = (b and 0xFF).toLong()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun SevenTvPaintGradientStopDto.toColorStop(): ColorStop =
    ColorStop(at = at, color = color.toArgbLong())

private fun SevenTvPaintShadowDto.toShadow(): Shadow = Shadow(
    xOffset = offsetX,
    yOffset = offsetY,
    radius = blur,
    color = color.toArgbLong()
)
