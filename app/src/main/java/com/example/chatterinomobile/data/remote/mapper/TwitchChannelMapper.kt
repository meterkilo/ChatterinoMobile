package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.Channel
import com.example.chatterinomobile.data.remote.dto.HelixStreamDto
import com.example.chatterinomobile.data.remote.dto.HelixUserDto

fun HelixUserDto.toChannel(stream: HelixStreamDto? = null): Channel = Channel(
    id = id,
    login = login,
    displayName = displayName,
    isLive = stream != null,
    viewerCount = stream?.viewerCount ?: 0,
    gameName = stream?.gameName,
    title = stream?.title,
    thumbnailUrl = stream?.thumbnailUrl?.replace("{width}", "1280")?.replace("{height}", "720")
)
