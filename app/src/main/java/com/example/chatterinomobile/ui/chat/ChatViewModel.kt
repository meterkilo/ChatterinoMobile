package com.example.chatterinomobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.ReplyMetadata
import com.example.chatterinomobile.data.model.SendMessageResult
import com.example.chatterinomobile.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the visible message buffer and send pipeline for a single active channel.
 *
 * Joined-channel selection lives in `ChannelTabsViewModel`; auth lives in
 * `AuthViewModel`. The screen-level glue calls [setActiveChannel] whenever the
 * user picks a different tab — this VM then:
 *   1. Resets the buffer.
 *   2. Seeds it with persisted scrollback from
 *      [ChatRepository.recentHistory], so reconnect/cold-start doesn't show
 *      an empty pane.
 *   3. Filters the live `messages` flow down to that channel's frames.
 *
 * The live-message collector is intentionally one job per active channel
 * rather than one global collector with an `if (msg.channelId == active)`
 * guard. Cancelling and relaunching is cheap (the underlying flow is
 * `shareIn`-backed) and it makes "we changed channels mid-stream" an
 * impossible state instead of a race window.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var liveCollector: Job? = null

    /**
     * Re-bind the visible buffer to a different channel.
     *
     * [channelId] is the Twitch room-id once hydration completes; pass null
     * before then. The id matters because PRIVMSG frames use it (not the
     * login) on the wire, and [ChatMessage.channelId] is whichever the
     * mapper had at parse time. Without it we'd miss messages on freshly
     * joined channels until hydration lands.
     */
    fun setActiveChannel(channelLogin: String?, channelId: String? = null) {
        val state = _uiState.value
        if (state.activeChannelLogin == channelLogin && state.activeChannelId == channelId) return

        val isChannelSwitch = state.activeChannelLogin != channelLogin
        liveCollector?.cancel()

        update {
            copy(
                activeChannelLogin = channelLogin,
                activeChannelId = channelId,
                // Only wipe the buffer if the user actually moved tabs — a
                // pure hydration update (login same, id newly known) must not
                // clear what's already on screen.
                recentMessages = if (isChannelSwitch) emptyList() else recentMessages,
                isLoadingHistory = isChannelSwitch && channelLogin != null,
                sendStatusMessage = null,
                sendErrorMessage = null
            )
        }

        if (channelLogin == null) return

        if (isChannelSwitch) {
            viewModelScope.launch {
                val history = runCatching { chatRepository.recentHistory(channelLogin) }
                    .getOrDefault(emptyList())
                if (_uiState.value.activeChannelLogin == channelLogin) {
                    update {
                        copy(
                            recentMessages = history.takeLast(MAX_RECENT_MESSAGES),
                            isLoadingHistory = false
                        )
                    }
                }
            }
        }

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

    // IrcMessageMapper uses the room-id tag when present, falling back to
    // the login. Match either form so freshly joined channels (id not yet
    // known) and post-hydration frames both flow into the buffer.
    private fun ChatMessage.belongsTo(activeLogin: String, activeId: String?): Boolean =
        channelId == activeLogin || (activeId != null && channelId == activeId)

    companion object {
        private const val MAX_RECENT_MESSAGES = 200
    }
}

data class ChatUiState(
    val activeChannelLogin: String? = null,
    val activeChannelId: String? = null,
    val isLoadingHistory: Boolean = false,
    val recentMessages: List<ChatMessage> = emptyList(),
    val sendStatusMessage: String? = null,
    val sendErrorMessage: String? = null
)

fun ReplyMetadata.describeParent(): String =
    parentDisplayName ?: parentUserLogin ?: parentMessageId
