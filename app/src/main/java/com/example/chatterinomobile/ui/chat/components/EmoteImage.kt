package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatterinomobile.data.model.Emote

@Composable
fun EmoteImage(
    emote: Emote,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val ratio = emote.aspectRatio ?: 1f
    val width = height * ratio.coerceAtLeast(0.4f).coerceAtMost(4f)
    val sizeMod = modifier.height(height).width(width)
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(emote.urls.forScale(2))
            .crossfade(false)
            .build(),
        contentDescription = emote.name,
        modifier = sizeMod
    )
}

