package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Badge

interface BadgeRepository {

    suspend fun loadGlobalBadges()

    suspend fun loadChannelBadges(channelId: String)

    fun findTwitchBadge(setId: String, version: String, channelId: String? = null): Badge?

    fun findThirdPartyBadges(twitchUserId: String): List<Badge>

    fun clearCache(channelId: String? = null)
}
