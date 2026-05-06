package com.example.chatterinomobile.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatterinomobile.data.local.DiscoverySnapshotCache
import com.example.chatterinomobile.data.local.FollowListCache
import com.example.chatterinomobile.data.model.Category
import com.example.chatterinomobile.data.model.Channel
import com.example.chatterinomobile.data.remote.api.TwitchHelixApi
import com.example.chatterinomobile.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val followedLive: List<Channel> = emptyList(),
    val followedLogins: List<String> = emptyList(),
    val recommendedStreams: List<Channel> = emptyList(),
    val topLiveStreams: List<Channel> = emptyList(),
    val topCategories: List<Category> = emptyList(),
    val activeCategory: Category? = null,
    val activeCategoryStreams: List<Channel> = emptyList(),
    val isLoadingCategoryStreams: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val searchActive: Boolean = false
)

class DiscoveryViewModel(
    private val helixApi: TwitchHelixApi,
    private val authRepository: AuthRepository,
    private val followListCache: FollowListCache,
    private val snapshotCache: DiscoverySnapshotCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    fun refresh() = load()

    fun openSearch() {
        _uiState.update { it.copy(searchActive = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchActive = false, searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            val results = runCatching { helixApi.searchChannels(query) }.getOrElse { emptyList() }
            val channels = results
                .map { dto ->
                    Channel(
                        id = dto.id,
                        login = dto.broadcasterLogin,
                        displayName = dto.displayName,
                        isLive = dto.isLive,
                        gameName = dto.gameName,
                        title = dto.title,
                        profileImageUrl = dto.thumbnailUrl
                    )
                }
                .sortedWith(
                    compareByDescending<Channel> { it.isLive }
                        .thenBy { it.displayName.lowercase() }
                )
            _uiState.update { it.copy(searchResults = channels, isSearching = false) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()
                val userKey = userId ?: ANON_KEY

                val snapshot = snapshotCache.read(userKey)
                val cachedLogins = if (userId != null) followListCache.read(userId)?.logins else null

                if (snapshot != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            followedLive = snapshot.followedLive,
                            followedLogins = cachedLogins ?: it.followedLogins,
                            recommendedStreams = snapshot.topLiveStreams.take(10),
                            topLiveStreams = snapshot.topLiveStreams,
                            topCategories = snapshot.topCategories,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    if (cachedLogins != null) {
                        _uiState.update { it.copy(followedLogins = cachedLogins) }
                    }
                }

                loadLiveAndRecommended(userId, cachedLogins, refreshFollows = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun loadLiveAndRecommended(
        userId: String?,
        logins: List<String>?,
        refreshFollows: Boolean
    ) {
        coroutineScope {
            val freshFollowsDeferred = if (refreshFollows && userId != null) {
                async { fetchAllFollows(userId) }
            } else null

            val topStreamsDeferred = async {
                runCatching { helixApi.getTopStreams(limit = 50) }.getOrElse { emptyList() }
            }

            val topGamesDeferred = async {
                runCatching { helixApi.getTopGames(limit = 30) }.getOrElse { emptyList() }
            }

            val followedLogins = if (freshFollowsDeferred != null) {
                val fresh = freshFollowsDeferred.await()
                if (userId != null && fresh.isNotEmpty()) followListCache.write(userId, fresh)
                fresh
            } else {
                logins ?: emptyList()
            }

            val topStreams = topStreamsDeferred.await()

            val followedLive: List<Channel> = if (followedLogins.isNotEmpty()) {
                val liveStreams = followedLogins
                    .chunked(100)
                    .map { batch ->
                        async {
                            runCatching { helixApi.getStreamsByLogin(batch) }.getOrElse { emptyList() }
                        }
                    }
                    .awaitAll()
                    .flatten()

                val liveLogins = liveStreams.map { it.userLogin }
                val usersByLogin = if (liveLogins.isNotEmpty()) {
                    liveLogins
                        .chunked(100)
                        .map { batch ->
                            async {
                                runCatching { helixApi.getUsersByLogin(batch) }.getOrElse { emptyList() }
                            }
                        }
                        .awaitAll()
                        .flatten()
                        .associateBy { it.login }
                } else emptyMap()

                val streamsByLogin = liveStreams.associateBy { it.userLogin }
                followedLogins
                    .mapNotNull { login -> streamsByLogin[login] }
                    .map { stream ->
                        val user = usersByLogin[stream.userLogin]
                        Channel(
                            id = stream.userId,
                            login = stream.userLogin,
                            displayName = stream.userName,
                            isLive = true,
                            viewerCount = stream.viewerCount,
                            gameName = stream.gameName,
                            title = stream.title,
                            thumbnailUrl = thumbnailUrl(stream.thumbnailUrl),
                            profileImageUrl = user?.profileImageUrl
                        )
                    }
            } else emptyList()

            val followedLoginSet = followedLogins.toSet()

            val topUsers = topStreams
                .map { it.userLogin }
                .chunked(100)
                .map { batch ->
                    async {
                        runCatching { helixApi.getUsersByLogin(batch) }.getOrElse { emptyList() }
                    }
                }
                .awaitAll()
                .flatten()
                .associateBy { it.login }

            val topLive = topStreams
                .sortedByDescending { it.viewerCount }
                .map { stream ->
                    Channel(
                        id = stream.userId,
                        login = stream.userLogin,
                        displayName = stream.userName,
                        isLive = true,
                        viewerCount = stream.viewerCount,
                        gameName = stream.gameName,
                        title = stream.title,
                        thumbnailUrl = thumbnailUrl(stream.thumbnailUrl),
                        profileImageUrl = topUsers[stream.userLogin]?.profileImageUrl
                    )
                }

            val recommended = topLive.filter { it.login !in followedLoginSet }.take(10)

            val viewersByGameId = topStreams
                .filter { it.gameId != null }
                .groupBy { it.gameId!! }
                .mapValues { (_, list) -> list.sumOf { it.viewerCount } }

            val topCategories = topGamesDeferred.await().map { game ->
                Category(
                    id = game.id,
                    name = game.name,
                    boxArtUrl = boxArtUrl(game.boxArtUrl),
                    viewerCount = viewersByGameId[game.id] ?: 0
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    followedLive = followedLive,
                    followedLogins = followedLogins,
                    recommendedStreams = recommended,
                    topLiveStreams = topLive,
                    topCategories = topCategories,
                    error = null
                )
            }

            runCatching {
                snapshotCache.write(
                    userKey = userId ?: ANON_KEY,
                    followedLive = followedLive,
                    topLiveStreams = topLive,
                    topCategories = topCategories
                )
            }
        }
    }

    private suspend fun fetchAllFollows(userId: String): List<String> {
        val all = mutableListOf<String>()
        var cursor: String? = null
        do {
            val page = runCatching {
                helixApi.getFollowedChannelsPaged(userId, after = cursor)
            }.getOrNull() ?: break
            all.addAll(page.logins)
            cursor = page.nextCursor
        } while (cursor != null && all.size < MAX_FOLLOWS)
        return all
    }

    private fun thumbnailUrl(raw: String?): String? {
        if (raw == null) return null
        val bucket = System.currentTimeMillis() / 1000 / 300
        return raw.replace("{width}", "440").replace("{height}", "248") + "?cb=$bucket"
    }

    private fun boxArtUrl(raw: String?): String? {
        if (raw == null) return null
        return raw.replace("{width}", "285").replace("{height}", "380")
    }

    private var categoryStreamsJob: Job? = null

    fun openCategory(category: Category) {
        categoryStreamsJob?.cancel()
        _uiState.update {
            it.copy(
                activeCategory = category,
                activeCategoryStreams = emptyList(),
                isLoadingCategoryStreams = true
            )
        }
        categoryStreamsJob = viewModelScope.launch {
            val streams = runCatching { helixApi.getStreamsByGameId(category.id, limit = 50) }
                .getOrElse { emptyList() }
                .sortedByDescending { it.viewerCount }
            val users = streams
                .map { it.userLogin }
                .chunked(100)
                .map { batch ->
                    runCatching { helixApi.getUsersByLogin(batch) }.getOrElse { emptyList() }
                }
                .flatten()
                .associateBy { it.login }
            val channels = streams.map { stream ->
                Channel(
                    id = stream.userId,
                    login = stream.userLogin,
                    displayName = stream.userName,
                    isLive = true,
                    viewerCount = stream.viewerCount,
                    gameName = stream.gameName,
                    title = stream.title,
                    thumbnailUrl = thumbnailUrl(stream.thumbnailUrl),
                    profileImageUrl = users[stream.userLogin]?.profileImageUrl
                )
            }
            _uiState.update {
                it.copy(
                    activeCategoryStreams = channels,
                    isLoadingCategoryStreams = false
                )
            }
        }
    }

    fun closeCategory() {
        categoryStreamsJob?.cancel()
        _uiState.update {
            it.copy(
                activeCategory = null,
                activeCategoryStreams = emptyList(),
                isLoadingCategoryStreams = false
            )
        }
    }

    companion object {
        private const val MAX_FOLLOWS = 1000
        private const val ANON_KEY = "anon"
    }
}
