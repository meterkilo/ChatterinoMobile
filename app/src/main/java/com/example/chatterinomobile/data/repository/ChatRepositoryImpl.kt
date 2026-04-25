package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.local.MessageHistoryStore
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.Channel
import com.example.chatterinomobile.data.model.ChannelHydrationState
import com.example.chatterinomobile.data.model.ModerationEvent
import com.example.chatterinomobile.data.model.RoomState
import com.example.chatterinomobile.data.model.SendMessageResult
import com.example.chatterinomobile.data.model.UserChatState
import com.example.chatterinomobile.data.remote.irc.IrcMessageMapper
import com.example.chatterinomobile.data.remote.irc.MessageEnricher
import com.example.chatterinomobile.data.remote.irc.ModerationEventMapper
import com.example.chatterinomobile.data.remote.irc.RoomStateMapper
import com.example.chatterinomobile.data.remote.irc.TwitchIrcClient
import com.example.chatterinomobile.data.remote.irc.UserStateMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default [ChatRepository]: demultiplexes the single IRC frame stream into
 * two typed flows.
 *
 *  - [messages]: PRIVMSG / USERNOTICE / NOTICE → [ChatMessage], then run
 *    through [MessageEnricher] to attach paints and third-party emote
 *    fragments.
 *  - [moderationEvents]: CLEARCHAT / CLEARMSG → [ModerationEvent] for the
 *    UI to apply as retroactive strikes / deletions.
 *
 * Everything else (JOIN, PART, PING, ROOMSTATE, USERSTATE, ...) is dropped
 * for now. Reconnection and rate-limiting will layer on top of this repo;
 * they don't belong inside [TwitchIrcClient].
 */
class ChatRepositoryImpl(
    private val ircClient: TwitchIrcClient,
    private val mapper: IrcMessageMapper,
    private val moderationMapper: ModerationEventMapper,
    private val roomStateMapper: RoomStateMapper,
    private val userStateMapper: UserStateMapper,
    private val enricher: MessageEnricher,
    private val channelRepository: ChannelRepository,
    private val badgeRepository: BadgeRepository,
    private val emoteRepository: EmoteRepository,
    private val historyStore: MessageHistoryStore
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val globalWarmupMutex = Mutex()
    private val connectMutex = Mutex()
    private val reconnectMutex = Mutex()
    private val joinedChannelMutex = Mutex()
    private val sendRateLimitMutex = Mutex()
    @Volatile
    private var globalsLoaded = false
    @Volatile
    private var shouldReconnect = false
    private val joinedChannelLogins = LinkedHashSet<String>()
    private val channelCacheByLogin = HashMap<String, Channel>()
    private val sendTimestampsByChannel = HashMap<String, ArrayDeque<Long>>()

    private val _roomStates = MutableStateFlow<Map<String, RoomState>>(emptyMap())
    override val roomStates: StateFlow<Map<String, RoomState>> = _roomStates.asStateFlow()

    private val _globalUserState = MutableStateFlow<UserChatState?>(null)
    override val globalUserState: StateFlow<UserChatState?> = _globalUserState.asStateFlow()

    private val _channelUserStates = MutableStateFlow<Map<String, UserChatState>>(emptyMap())
    override val channelUserStates: StateFlow<Map<String, UserChatState>> =
        _channelUserStates.asStateFlow()

    private val _channelHydrationStates = MutableStateFlow<Map<String, ChannelHydrationState>>(emptyMap())
    override val channelHydrationStates: StateFlow<Map<String, ChannelHydrationState>> =
        _channelHydrationStates.asStateFlow()

    init {
        scope.launch {
            ircClient.incoming.collect { raw ->
                roomStateMapper.map(raw)?.let { state ->
                    _roomStates.value = _roomStates.value + mapOf(
                        state.channelId to state,
                        state.channelLogin to state
                    )
                }

                userStateMapper.map(raw)?.let { state ->
                    if (state.channelId == null && state.channelLogin == null) {
                        _globalUserState.value = state
                    } else {
                        val updates = buildMap {
                            state.channelId?.let { put(it, state) }
                            state.channelLogin?.let { put(it, state) }
                        }
                        if (updates.isNotEmpty()) {
                            _channelUserStates.value = _channelUserStates.value + updates
                        }
                    }
                }
            }
        }

        scope.launch {
            ircClient.disconnects.collect {
                if (shouldReconnect) reconnectLoop()
            }
        }
    }

    /**
     * The enriched message stream is shared (`shareIn`) so we don't re-run
     * the mapper + enricher once per UI collector. Crucially, the side-effect
     * `onEach { historyStore.enqueue(...) }` runs in the upstream — *before*
     * sharing — which means persistence happens exactly once per message
     * regardless of how many ViewModels are observing.
     *
     * Replay 0 because cold-start scrollback is served explicitly via
     * [recentHistory]; we don't want late subscribers to re-receive a
     * snapshot of in-flight live messages and double-render.
     */
    override val messages: Flow<ChatMessage> =
        ircClient.incoming
            .mapNotNull { raw -> mapper.map(raw) }
            .map { msg -> enricher.enrich(msg) }
            .onEach { msg -> historyStore.enqueue(msg) }
            .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    override val moderationEvents: Flow<ModerationEvent> =
        ircClient.incoming
            .mapNotNull { raw -> moderationMapper.map(raw) }
            .onEach { event -> mirrorModerationToHistory(event) }
            .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    override suspend fun recentHistory(channelLogin: String, limit: Int): List<ChatMessage> {
        val normalizedLogin = channelLogin.lowercase().removePrefix("#")
        // History is keyed by Twitch channel ID, not login, because logins
        // are mutable. If the user is loading scrollback for a channel they
        // haven't joined yet in this process we need to resolve the login
        // first; the cache hit case skips the network call.
        val channel = resolveChannel(normalizedLogin) ?: return emptyList()
        return historyStore.recent(channel.id, limit)
    }

    /**
     * Mirror moderation events into the persisted log so scrollback after a
     * rejoin honors the same redactions the user already saw live. We
     * deliberately resolve the channel lazily (and only for non-trivial
     * variants) to avoid a Helix call for every delete event.
     */
    private fun mirrorModerationToHistory(event: ModerationEvent) {
        scope.launch {
            runCatching {
                when (event) {
                    is ModerationEvent.ChatCleared -> {
                        val channel = channelCacheByLogin[event.channelLogin]
                            ?: channelRepository.getChannelByLogin(event.channelLogin)
                            ?: return@runCatching
                        historyStore.deleteByChannel(channel.id)
                    }
                    is ModerationEvent.UserBanned -> {
                        val channel = channelCacheByLogin[event.channelLogin]
                            ?: channelRepository.getChannelByLogin(event.channelLogin)
                            ?: return@runCatching
                        historyStore.deleteByChannelAndAuthor(channel.id, event.targetUserId)
                    }
                    is ModerationEvent.UserTimedOut -> {
                        val channel = channelCacheByLogin[event.channelLogin]
                            ?: channelRepository.getChannelByLogin(event.channelLogin)
                            ?: return@runCatching
                        historyStore.deleteByChannelAndAuthor(channel.id, event.targetUserId)
                    }
                    is ModerationEvent.MessageDeleted -> {
                        historyStore.deleteByMessageId(event.targetMessageId)
                    }
                }
            }
        }
    }

    override suspend fun connect() {
        shouldReconnect = true
        connectMutex.withLock {
            ircClient.connect()
        }
        ensureGlobalCachesLoadedAsync()
    }

    override suspend fun joinChannel(channelLogin: String) {
        val normalizedLogin = channelLogin.lowercase().removePrefix("#")
        joinedChannelMutex.withLock {
            joinedChannelLogins.add(normalizedLogin)
        }
        _channelHydrationStates.value = _channelHydrationStates.value + (
            normalizedLogin to ChannelHydrationState(
                channelLogin = normalizedLogin,
                isLoading = true,
                isReady = false,
                errorMessage = null
            )
        )
        ircClient.joinChannel(normalizedLogin)
        ensureGlobalCachesLoadedAsync()
        hydrateChannelCachesAsync(normalizedLogin)
    }

    override suspend fun leaveChannel(channelLogin: String) {
        val normalizedLogin = channelLogin.lowercase().removePrefix("#")
        joinedChannelMutex.withLock {
            joinedChannelLogins.remove(normalizedLogin)
        }
        ircClient.partChannel(normalizedLogin)
    }

    override suspend fun sendMessage(channelLogin: String, text: String): SendMessageResult {
        val normalizedLogin = channelLogin.lowercase().removePrefix("#")
        if (text.isBlank()) return SendMessageResult.EmptyMessage
        awaitSendPermit(normalizedLogin)
        return ircClient.sendMessage(normalizedLogin, text)
    }

    override suspend fun disconnect() {
        shouldReconnect = false
        ircClient.disconnect()
    }

    /**
     * Global badges/emotes are shared across every channel, so they should be
     * loaded once before the first join rather than re-fetching them per tab.
     */
    private suspend fun ensureGlobalCachesLoaded() {
        if (globalsLoaded) return
        globalWarmupMutex.withLock {
            if (globalsLoaded) return
            coroutineScope {
                val badges = async { badgeRepository.loadGlobalBadges() }
                val emotes = async { emoteRepository.loadEmotesForChannel(channelId = null) }
                badges.await()
                emotes.await()
            }
            globalsLoaded = true
        }
    }

    private fun ensureGlobalCachesLoadedAsync() {
        if (globalsLoaded) return
        scope.launch {
            runCatching { ensureGlobalCachesLoaded() }
        }
    }

    private suspend fun reconnectLoop() {
        reconnectMutex.withLock {
            var attempt = 0
            while (shouldReconnect) {
                val result = runCatching {
                    connectMutex.withLock {
                        ircClient.connect()
                    }
                    rejoinTrackedChannels()
                }

                if (result.isSuccess) return

                val backoffMillis =
                    (1_000L * (1L shl attempt.coerceAtMost(5))).coerceAtMost(30_000L)
                delay(backoffMillis)
                attempt++
            }
        }
    }

    private suspend fun rejoinTrackedChannels() {
        val channels = joinedChannelMutex.withLock { joinedChannelLogins.toList() }
        for (channelLogin in channels) {
            ircClient.joinChannel(channelLogin)
            hydrateChannelCachesAsync(channelLogin)
        }
    }

    /**
     * Joining IRC should be low-latency. We therefore resolve Helix identity
     * and load third-party caches in the background after the JOIN frame has
     * already been sent instead of blocking chat startup on multiple APIs.
     */
    private fun hydrateChannelCachesAsync(channelLogin: String) {
        scope.launch {
            runCatching {
                val channel = resolveChannel(channelLogin) ?: error("Channel not found")
                _channelHydrationStates.value = _channelHydrationStates.value + (
                    channelLogin to ChannelHydrationState(
                        channelLogin = channelLogin,
                        channelId = channel.id,
                        isLoading = true,
                        isReady = false,
                        errorMessage = null
                    )
                )

                coroutineScope {
                    val badges = async { badgeRepository.loadChannelBadges(channel.id) }
                    val emotes = async { emoteRepository.loadEmotesForChannel(channel.id) }
                    badges.await()
                    emotes.await()
                }

                _channelHydrationStates.value = _channelHydrationStates.value + (
                    channelLogin to ChannelHydrationState(
                        channelLogin = channelLogin,
                        channelId = channel.id,
                        isLoading = false,
                        isReady = true,
                        errorMessage = null
                    )
                )
            }.onFailure { throwable ->
                val channelId = channelCacheByLogin[channelLogin]?.id
                _channelHydrationStates.value = _channelHydrationStates.value + (
                    channelLogin to ChannelHydrationState(
                        channelLogin = channelLogin,
                        channelId = channelId,
                        isLoading = false,
                        isReady = false,
                        errorMessage = throwable.message ?: "Failed to hydrate channel"
                    )
                )
            }
        }
    }

    private suspend fun resolveChannel(channelLogin: String): Channel? {
        channelCacheByLogin[channelLogin]?.let { return it }
        val channel = channelRepository.getChannelByLogin(channelLogin) ?: return null
        channelCacheByLogin[channelLogin] = channel
        return channel
    }

    /**
     * Twitch's baseline user rate limit is 20 PRIVMSGs per 30 seconds. We use
     * a tiny sliding-window limiter here so callers naturally backpressure
     * instead of writing directly to the socket and getting disconnected.
     */
    private suspend fun awaitSendPermit(channelLogin: String) {
        while (true) {
            val waitMillis = sendRateLimitMutex.withLock {
                val now = System.currentTimeMillis()
                val timestamps = sendTimestampsByChannel.getOrPut(channelLogin) { ArrayDeque() }
                while (timestamps.isNotEmpty() && now - timestamps.first() >= SEND_WINDOW_MILLIS) {
                    timestamps.removeFirst()
                }

                if (timestamps.size < MAX_MESSAGES_PER_WINDOW) {
                    timestamps.addLast(now)
                    0L
                } else {
                    val oldest = timestamps.first()
                    (SEND_WINDOW_MILLIS - (now - oldest)).coerceAtLeast(1L)
                }
            }

            if (waitMillis == 0L) return
            delay(waitMillis)
        }
    }

    companion object {
        private const val MAX_MESSAGES_PER_WINDOW = 20
        private const val SEND_WINDOW_MILLIS = 30_000L
    }
}
