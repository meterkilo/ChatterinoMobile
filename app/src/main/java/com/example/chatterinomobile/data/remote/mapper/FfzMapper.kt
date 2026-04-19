package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.model.EmoteProvider
import com.example.chatterinomobile.data.model.EmoteUrls
import com.example.chatterinomobile.data.remote.dto.FfzEmoteDto

fun FfzEmoteDto.toDomain(): Emote {
    // FFZ URLs are protocol-relative ("//cdn.frankerfacez.com/...")
    val sourceUrls = animated ?: urls  // prefer animated URL set if present

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

/**
 * FFZ URLs are protocol-relative (start with "//"). Prepend "https:"
 * and handle the rare null case by returning an empty string.
 */
private fun normalize(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return if (url.startsWith("//")) "https:$url" else url
}