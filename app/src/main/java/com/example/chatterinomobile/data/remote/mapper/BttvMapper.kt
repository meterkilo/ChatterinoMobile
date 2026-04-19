package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.model.EmoteProvider
import com.example.chatterinomobile.data.model.EmoteUrls
import com.example.chatterinomobile.data.remote.dto.BttvEmoteDto

fun BttvEmoteDto.toDomain(): Emote {
    val baseUrl = "https://cdn.betterttv.net/emote/$id"

    return Emote(
        id = id,
        name = code,
        urls = EmoteUrls(
            x1 = "$baseUrl/1x.webp",
            x2 = "$baseUrl/2x.webp",
            x3 = "$baseUrl/3x.webp",
            x4 = "$baseUrl/3x.webp"
        ),
        isAnimated = animated,
        isZeroWidth = false,
        provider = EmoteProvider.BTTV
    )
}