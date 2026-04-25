package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

/**
 * A single chat message, fully parsed and ready to render.
 *
 * `@Serializable` on the type (and on [MessageType], [MessageFragment],
 * [ChatUser], [ReplyMetadata], [Paint]) lets
 * [com.example.chatterinomobile.data.local.MessageHistoryStore] persist an
 * exact snapshot for scrollback — see that class for why we store enriched
 * fragments rather than re-enriching on read.
 **/


@Serializable
data class ChatMessage(
    val id: String,
    val channelId: String,
    val author : ChatUser,
    val reply: ReplyMetadata? = null,
    val fragment: List<MessageFragment>,
    val badges: List<Badge> = emptyList(),
    val timestamp: Long,
    val Type: MessageType = MessageType.Regular
)

/**
 * Distinguishes rendering variants of a message without duplicating the parser.
 */
@Serializable
sealed class MessageType {
    /** Normal message. */
    @Serializable
    data object Regular : MessageType()

    /** /me action — rendered italic, author color applied to body. */
    @Serializable
    data object Action: MessageType()

    /** Channel-point highlighted message — rendered with accent background. */
    @Serializable
    data object Highlighted: MessageType()

    /** Reply to another message. */
    @Serializable
    data class Reply(val parentId : String) : MessageType()

    /** System notice ("User has been banned", etc). */
    @Serializable
    data class System(val text: String) : MessageType()
}
