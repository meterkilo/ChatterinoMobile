package com.example.chatterinomobile.data.local

import android.content.Context
import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.ChatUser
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.data.model.MessageType
import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.model.ReplyMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class MessageHistoryStore(
    context: Context,
    scopeOverride: CoroutineScope? = null
) {
    private val perChannelMessages = HashMap<String, ArrayDeque<ChatMessage>>()
    private val messagesByMessageId = HashMap<String, ChatMessage>()

    private val mutex = Mutex()

    fun enqueue(message: ChatMessage) {
        val buffer = perChannelMessages.getOrPut(message.channelId) { ArrayDeque() }
        buffer.addLast(message)
        if (buffer.size > MAX_MESSAGES_PER_CHANNEL) {
            buffer.removeFirst()
        }
        messagesByMessageId[message.id] = message
    }

    suspend fun deleteByMessageId(twitchMessageId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val message = messagesByMessageId.remove(twitchMessageId)
            if (message != null) {
                perChannelMessages[message.channelId]?.removeAll { it.id == twitchMessageId }
            }
        }
    }

    suspend fun deleteByChannelAndAuthor(channelId: String, authorId: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val buffer = perChannelMessages[channelId] ?: return@withLock
                buffer.removeAll { it.author.id == authorId }
            }
        }

    suspend fun deleteByChannel(channelId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            perChannelMessages.remove(channelId)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            perChannelMessages.clear()
            messagesByMessageId.clear()
        }
    }

    suspend fun recent(
        channelId: String,
        limit: Int = DEFAULT_SCROLLBACK_LIMIT
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            perChannelMessages[channelId]?.takeLast(limit)?.toList() ?: emptyList()
        }
    }

    fun shutdown() {

    }

    companion object {
        private const val DATABASE_NAME = "chatterino_history.db"

        private const val MAX_MESSAGES_PER_CHANNEL = 2_000

        private const val TRIM_EVERY_N_INSERTS = 200

        private const val DEFAULT_SCROLLBACK_LIMIT = 500
    }
}
