package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a user in chat (the message author).
 *
 * - [id] is the immutable Twitch user ID. Always key logic off this, never [login].
 * - [login] is the lowercase username (changeable by the user).
 * - [displayName] is what's shown in the UI (can include capitalization / unicode).
 * - [color] is the user's chosen Twitch username color as a hex string, or null if default.
 * - [paint] is the 7TV cosmetic overlay for the username, if any. Shadows live inside the paint.
 *
 * `@Serializable` so persisted chat history remains faithful to the author's
 * identity at the moment the message was received.
 */
@Serializable
data class ChatUser(
    val id : String,
    val login: String,
    val displayName: String,
    val color: String? = null,
    val paint: Paint? = null
)
