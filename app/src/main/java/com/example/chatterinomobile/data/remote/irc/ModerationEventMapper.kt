package com.example.chatterinomobile.data.remote.irc

import com.example.chatterinomobile.data.model.ModerationEvent

class ModerationEventMapper {

    fun map(raw: IrcMessage): ModerationEvent? = when (raw.command) {
        "CLEARCHAT" -> mapClearChat(raw)
        "CLEARMSG" -> mapClearMsg(raw)
        else -> null
    }

    private fun mapClearChat(raw: IrcMessage): ModerationEvent? {
        val channelLogin = raw.channel ?: return null

        val targetLogin = if (raw.params.size > 1) raw.trailing else null

        if (targetLogin.isNullOrBlank()) {
            return ModerationEvent.ChatCleared(channelLogin)
        }

        val targetUserId = raw.tags["target-user-id"] ?: return null
        val duration = raw.tags["ban-duration"]?.toIntOrNull()

        return if (duration != null) {
            ModerationEvent.UserTimedOut(
                channelLogin = channelLogin,
                targetUserId = targetUserId,
                targetLogin = targetLogin,
                durationSeconds = duration
            )
        } else {
            ModerationEvent.UserBanned(
                channelLogin = channelLogin,
                targetUserId = targetUserId,
                targetLogin = targetLogin
            )
        }
    }

    private fun mapClearMsg(raw: IrcMessage): ModerationEvent? {
        val channelLogin = raw.channel ?: return null
        val targetMessageId = raw.tags["target-msg-id"]?.takeIf { it.isNotBlank() } ?: return null
        val targetLogin = raw.tags["login"]?.takeIf { it.isNotBlank() } ?: return null
        return ModerationEvent.MessageDeleted(
            channelLogin = channelLogin,
            targetMessageId = targetMessageId,
            targetLogin = targetLogin
        )
    }
}
