package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.model.EmoteProvider
import com.example.chatterinomobile.data.model.EmoteUrls
import com.example.chatterinomobile.data.remote.dto.FfzEmoteDto

fun FfzEmoteDto.toDomain(): Emote {

    val sourceUrls = animated ?: urls

    return Emote(
        id = id.toString(),
        name = name,
        urls = EmoteUrls(
            x1 = normalize(sourceUrls["1"]),
            x2 = normalize(sourceUrls["2"] ?: sourceUrls["1"]),
            x3 = normalize(sourceUrls["4"] ?: sourceUrls["2"] ?: sourceUrls["1"]),
            x4 = normalize(sourceUrls["4"] ?: sourceUrls["2"] ?: sourceUrls["1"])
        ),
        isAnimated = animated != null,
        isZeroWidth = false,
        provider = EmoteProvider.FFZ
    )
}

private fun normalize(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return if (url.startsWith("//")) "https:$url" else url
}
