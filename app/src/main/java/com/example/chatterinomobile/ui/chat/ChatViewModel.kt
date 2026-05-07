    package com.example.chatterinomobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.ModerationEvent
import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.model.ReplyMetadata
import com.example.chatterinomobile.data.model.SendMessageResult
import com.example.chatterinomobile.data.repository.ChatRepository
import com.example.chatterinomobile.data.repository.PaintRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val paintRepository: PaintRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(paintsByUserId = paintRepository.snapshot()))
    val uiState = _uiState.asStateFlow()

    private var liveCollector: Job? = null
    private var moderationCollector: Job? = null

    init {
        moderationCollector = viewModelScope.launch {
            chatRepository.moderationEvents.collect { event ->
                val state = _uiState.value
                val active = state.activeChannelLogin ?: return@collect
                if (event.channelLogin != active) return@collect
                when (event) {
                    is ModerationEvent.ChatCleared -> update { copy(deletedIds = emptySet(), bannedLogins = emptySet()) }
                    is ModerationEvent.MessageDeleted -> update {
                        copy(deletedIds = deletedIds + event.targetMessageId)
                    }
                    is ModerationEvent.UserBanned -> update {
                        copy(bannedLogins = bannedLogins + event.targetLogin)
                    }
                    is ModerationEvent.UserTimedOut -> update {
                        copy(bannedLogins = bannedLogins + event.targetLogin)
                    }
                }
            }
        }

        viewModelScope.launch {
            paintRepository.paintAssignments.collect { assignment ->
                update {
                    copy(paintsByUserId = paintsByUserId + (assignment.twitchUserId to assignment.paint))
                }
            }
        }
    }

    fun setActiveChannel(channelLogin: String?, channelId: String? = null) {
        val state = _uiState.value
        if (state.activeChannelLogin == channelLogin && state.activeChannelId == channelId) return

        val isChannelSwitch = state.activeChannelLogin != channelLogin
        liveCollector?.cancel()

        update {
            copy(
                activeChannelLogin = channelLogin,
                activeChannelId = channelId,
                recentMessages = if (isChannelSwitch) emptyList() else recentMessages,
                deletedIds = if (isChannelSwitch) emptySet() else deletedIds,
                bannedLogins = if (isChannelSwitch) emptySet() else bannedLogins,
                sendStatusMessage = null,
                sendErrorMessage = null
            )
        }

        if (channelLogin == null) return

        liveCollector = viewModelScope.launch {
            chatRepository.messages.collect { message ->
                val current = _uiState.value
                val active = current.activeChannelLogin ?: return@collect
                if (!message.belongsTo(active, current.activeChannelId)) return@collect
                update {
                    copy(recentMessages = (recentMessages + message).takeLast(MAX_RECENT_MESSAGES))
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val active = _uiState.value.activeChannelLogin ?: run {
            update { copy(sendErrorMessage = "Join a channel first.") }
            return
        }

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(active, text)) {
                SendMessageResult.Sent -> update {
                    copy(sendStatusMessage = "Message sent.", sendErrorMessage = null)
                }
                SendMessageResult.EmptyMessage -> update {
                    copy(sendErrorMessage = "Message cannot be empty.")
                }
                SendMessageResult.Anonymous -> update {
                    copy(sendErrorMessage = "Sign in to send chat messages.")
                }
                SendMessageResult.Disconnected -> update {
                    copy(sendErrorMessage = "Chat socket is disconnected.")
                }
                is SendMessageResult.Failed -> update {
                    copy(sendErrorMessage = result.message)
                }
            }
        }
    }

    fun consumeSendMessages() {
        update { copy(sendStatusMessage = null, sendErrorMessage = null) }
    }

    private inline fun update(transform: ChatUiState.() -> ChatUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun ChatMessage.belongsTo(activeLogin: String, activeId: String?): Boolean =
        channelId == activeLogin || (activeId != null && channelId == activeId)

    companion object {
        private const val MAX_RECENT_MESSAGES = 200
    }
}

data class ChatUiState(
    val activeChannelLogin: String? = null,
    val activeChannelId: String? = null,
    val recentMessages: List<ChatMessage> = emptyList(),
    val deletedIds: Set<String> = emptySet(),
    val bannedLogins: Set<String> = emptySet(),
    val paintsByUserId: Map<String, Paint> = emptyMap(),
    val sendStatusMessage: String? = null,
    val sendErrorMessage: String? = null
)

fun ReplyMetadata.describeParent(): String =
    parentDisplayName ?: parentUserLogin ?: parentMessageId
