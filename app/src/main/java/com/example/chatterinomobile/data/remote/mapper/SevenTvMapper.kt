package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.model.EmoteProvider
import com.example.chatterinomobile.data.model.EmoteUrls
import com.example.chatterinomobile.data.remote.dto.SevenTvActiveEmoteDto
import com.example.chatterinomobile.data.remote.dto.SevenTvFileDto
import com.example.chatterinomobile.data.remote.dto.SevenTvHostDto

fun SevenTvActiveEmoteDto.toDomain(): Emote {
    val host = data.host
    val files = host.files

    return Emote(
        id = id,
        name = name,
        urls = EmoteUrls(
            x1 = buildUrl(host, pickFile(files, "1x")),
            x2 = buildUrl(host, pickFile(files, "2x")),
            x3 = buildUrl(host, pickFile(files, "3x")),
            x4 = buildUrl(host, pickFile(files, "4x"))
        ),
        isAnimated = data.animated,
        isZeroWidth = (flags and 1) != 0,
        provider = EmoteProvider.SEVENTV
    )
}

private fun buildUrl(host: SevenTvHostDto, file: SevenTvFileDto): String {
    return "https:${host.url}/${file.name}"
}

private fun pickFile(files: List<SevenTvFileDto>, scale: String): SevenTvFileDto {
    return files.find { it.name.startsWith(scale) } ?: files.first()
}