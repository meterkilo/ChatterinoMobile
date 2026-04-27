package com.example.chatterinomobile.data.model

sealed class ModerationEvent {

    abstract val channelLogin: String

    data class ChatCleared(
        override val channelLogin: String
    ) : ModerationEvent()

    data class UserBanned(
        override val channelLogin: String,
        val targetUserId: String,
        val targetLogin: String
    ) : ModerationEvent()

    data class UserTimedOut(
        override val channelLogin: String,
        val targetUserId: String,
        val targetLogin: String,
        val durationSeconds: Int
    ) : ModerationEvent()

    data class MessageDeleted(
        override val channelLogin: String,
        val targetMessageId: String,
        val targetLogin: String
    ) : ModerationEvent()
}
