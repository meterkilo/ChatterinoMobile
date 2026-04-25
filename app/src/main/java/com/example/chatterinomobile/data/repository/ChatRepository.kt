package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.ChannelHydrationState
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.ModerationEvent
import com.example.chatterinomobile.data.model.RoomState
import com.example.chatterinomobile.data.model.SendMessageResult
import com.example.chatterinomobile.data.model.UserChatState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates the IRC WebSocket lifecycle and exposes a single hot [Flow] of
 * rendered [ChatMessage]s for the UI.
 *
 * Callers should:
 *   1. Call [connect] once (e.g. from a long-lived scope like `Application`).
 *   2. [joinChannel] for each channel tab they open, [leaveChannel] when closed.
 *   3. Collect [messages], optionally filtered by `channelId` / `channelLogin`.
 *
 * The repository is deliberately agnostic about reconnection strategy —
 * implementations can add backoff, network-state observers, etc. behind
 * this interface without ripping up the UI.
 */
interface ChatRepository {

    /**
     * Hot flow of every visible chat row across all joined channels — regular
     * PRIVMSGs as well as system rows (USERNOTICE, NOTICE). Already enriched
     * with paints and third-party emote fragments.
     */
    val messages: Flow<ChatMessage>

    /**
     * Hot flow of retroactive chat mutations (bans, timeouts, single-message
     * deletes, full chat clears). The UI must reach back into [messages] it
     * has already rendered and apply these.
     */
    val moderationEvents: Flow<ModerationEvent>

    val roomStates: StateFlow<Map<String, RoomState>>

    val globalUserState: StateFlow<UserChatState?>

    val channelUserStates: StateFlow<Map<String, UserChatState>>

    val channelHydrationStates: StateFlow<Map<String, ChannelHydrationState>>

    /**
     * Returns the most recent persisted messages for [channelLogin], oldest-
     * first. ViewModels emit these into the UI before any new live messages
     * arrive, giving cold-start scrollback without a network round trip.
     *
     * The login → channel-id resolution is handled internally; if we have
     * never resolved that login (no prior join) the result is empty rather
     * than blocking on Helix.
     */
    suspend fun recentHistory(channelLogin: String, limit: Int = 500): List<ChatMessage>

    /** Idempotent. Establishes the WebSocket + logs in (anonymously, for now). */
    suspend fun connect()

    /** Subscribe to a channel by login (the lowercase username). */
    suspend fun joinChannel(channelLogin: String)

    /** Unsubscribe. No-op if we weren't in the channel. */
    suspend fun leaveChannel(channelLogin: String)

    /**
     * Send a chat message. Returns false when anonymous (no token) or when
     * the socket isn't connected.
     */
    suspend fun sendMessage(channelLogin: String, text: String): SendMessageResult

    /** Tear down the socket and cancel the reader. Safe to call multiple times. */
    suspend fun disconnect()
}
