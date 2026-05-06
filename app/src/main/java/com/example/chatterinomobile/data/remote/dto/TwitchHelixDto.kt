package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelixListResponse<T>(
    val data: List<T> = emptyList()
)

@Serializable
data class HelixUserDto(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    val description: String? = null
)

@Serializable
data class HelixBadgeSetDto(
    @SerialName("set_id") val setId: String,
    val versions: List<HelixBadgeVersionDto>
)

@Serializable
data class HelixBadgeVersionDto(
    val id: String,
    @SerialName("image_url_1x") val imageUrl1x: String,
    @SerialName("image_url_2x") val imageUrl2x: String,
    @SerialName("image_url_4x") val imageUrl4x: String,
    val title: String,
    val description: String,
    @SerialName("click_action") val clickAction: String? = null,
    @SerialName("click_url") val clickUrl: String? = null
)

@Serializable
data class HelixStreamDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String,
    @SerialName("game_id") val gameId: String? = null,
    @SerialName("game_name") val gameName: String? = null,
    val type: String,
    val title: String,
    @SerialName("viewer_count") val viewerCount: Int,
    @SerialName("started_at") val startedAt: String,
    val language: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class HelixListResponseWithPagination<T>(
    val data: List<T> = emptyList(),
    val pagination: HelixPagination? = null,
    val total: Int? = null
)

@Serializable
data class HelixPagination(
    val cursor: String? = null
)

@Serializable
data class HelixFollowedChannelDto(
    @SerialName("broadcaster_id") val broadcasterId: String,
    @SerialName("broadcaster_login") val broadcasterLogin: String,
    @SerialName("broadcaster_name") val broadcasterName: String,
    @SerialName("followed_at") val followedAt: String
)

@Serializable
data class HelixGameDto(
    val id: String,
    val name: String,
    @SerialName("box_art_url") val boxArtUrl: String? = null,
    @SerialName("igdb_id") val igdbId: String? = null
)

@Serializable
data class HelixSearchChannelDto(
    val id: String,
    @SerialName("broadcaster_login") val broadcasterLogin: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_live") val isLive: Boolean = false,
    @SerialName("game_name") val gameName: String? = null,
    val title: String? = null
)
