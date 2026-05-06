package com.example.chatterinomobile.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chatterinomobile.data.model.Channel
import com.example.chatterinomobile.ui.theme.Twick
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

@Composable
fun DiscoveryScreen(
    onJoinChannel: (String) -> Unit,
    onRemovePin: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    pinnedChannelLogins: List<String> = emptyList(),
    viewModel: DiscoveryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Twick.Bg)
    ) {
        if (state.searchActive) {
            SearchBody(
                state = state,
                onQueryChange = viewModel::onSearchQueryChange,
                onClose = viewModel::closeSearch,
                onJoinChannel = { login ->
                    viewModel.closeSearch()
                    onJoinChannel(login)
                }
            )
            return@Column
        }
        if (activeTab != 0) {
            DiscoveryTopBar(onSearch = viewModel::openSearch)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp)
            ) {
                when {
                    state.isLoading -> LoadingBody()
                    state.error != null && state.followedLive.isEmpty() && state.recommendedStreams.isEmpty() ->
                        ErrorBody(message = state.error!!, onRetry = viewModel::refresh)
                    else -> when (activeTab) {
                        0 -> HomeBody(
                            state = state,
                            pinnedChannelLogins = pinnedChannelLogins,
                            onJoinChannel = onJoinChannel,
                            onRemovePin = onRemovePin,
                            onSearch = viewModel::openSearch,
                            onRefresh = viewModel::refresh
                        )
                        1 -> BrowseBody(state = state, onJoinChannel = onJoinChannel)
                        2 -> YouBody()
                        else -> BrowseBody(state = state, onJoinChannel = onJoinChannel)
                    }
                }
            }

            DiscoveryBottomBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SearchBody(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onJoinChannel: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = onQueryChange,
            onClose = onClose
        )

        when {
            state.searchQuery.isBlank() -> SearchEmptyText(text = "Search channels")
            state.isSearching -> LoadingBody()
            state.searchResults.isEmpty() -> SearchEmptyText(text = "No channels found")
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                state.searchResults.forEach { channel ->
                    SearchResultRow(channel = channel, onClick = { onJoinChannel(channel.login) })
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TopBarIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onClose)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Twick.S1)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Twick.Ink3, modifier = Modifier.size(19.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = "Channel name", color = Twick.Ink4, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Twick.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onQueryChange("") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Twick.Ink3, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Twick.Hairline)
    )
}

@Composable
private fun SearchEmptyText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = Twick.Ink3, fontSize = 14.sp)
    }
}

@Composable
private fun SearchResultRow(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            AvatarCircle(name = channel.displayName, size = 44.dp, imageUrl = channel.profileImageUrl)
            if (channel.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Twick.Live)
                        .border(2.dp, Twick.Bg, CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = channel.displayName,
                    color = Twick.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = if (channel.isLive) "LIVE" else "OFFLINE",
                    color = if (channel.isLive) Color.White else Twick.Ink3,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (channel.isLive) Twick.Live else Twick.S2)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                text = buildSearchSubtitle(channel),
                color = Twick.Ink3,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildSearchSubtitle(channel: Channel): String =
    when {
        channel.isLive && channel.gameName != null -> channel.gameName
        channel.isLive -> "Live"
        else -> "@${channel.login}"
    }

@Composable
private fun LoadingBody() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Twick.Accent, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Couldn't load streams", color = Twick.Ink2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text = message, color = Twick.Ink3, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Twick.S2)
                .clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Twick.Accent, modifier = Modifier.size(16.dp))
            Text(text = "Try again", color = Twick.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ForYouBody(
    state: DiscoveryUiState,
    onJoinChannel: (String) -> Unit,
    onSeeAllFollowing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (state.followedLive.isNotEmpty()) {
            SectionHeader(title = "Live now · following", action = "See all", onActionClick = onSeeAllFollowing)
            LiveFollowingRow(channels = state.followedLive, onClick = onJoinChannel)
            Spacer(Modifier.height(8.dp))
        }

        if (state.recommendedStreams.isNotEmpty()) {
            SectionHeader(title = "Recommended")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.recommendedStreams.forEach { channel ->
                    StreamCard(channel = channel, onClick = { onJoinChannel(channel.login) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FollowingBody(state: DiscoveryUiState, onJoinChannel: (String) -> Unit) {
    val liveLogins = state.followedLive.map { it.login }.toSet()

    if (state.followedLogins.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You're not following anyone yet", color = Twick.Ink3, fontSize = 14.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        state.followedLive.forEach { channel ->
            FollowingRow(channel = channel, isLive = true, onClick = { onJoinChannel(channel.login) })
        }

        state.followedLogins
            .filter { it !in liveLogins }
            .take(30)
            .forEach { login ->
                val offlineChannel = Channel(id = "", login = login, displayName = login)
                FollowingRow(channel = offlineChannel, isLive = false, onClick = { onJoinChannel(login) })
            }
        Spacer(Modifier.height(16.dp))
    }
}

private enum class PinnedKind { Channel, Chat }

private data class PinnedItem(
    val kind: PinnedKind,
    val login: String,
    val name: String,
    val subtitle: String,
    val message: String? = null,
    val platform: String? = "twitch",
    val isLive: Boolean = false,
    val viewers: String? = null,
    val imageUrl: String? = null,
    val unread: Int = 0,
    val mention: Boolean = false,
    val scheduled: String? = null
)

private fun buildHomePinnedItems(state: DiscoveryUiState, pinnedChannelLogins: List<String>): List<PinnedItem> {
    val channelsByLogin = (state.followedLive + state.recommendedStreams + state.searchResults)
        .distinctBy { it.login.lowercase() }
        .associateBy { it.login.lowercase() }
    return pinnedChannelLogins
        .map { it.lowercase().removePrefix("#").trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { login ->
            channelsByLogin[login]?.toPinnedItem()
                ?: PinnedItem(
                    kind = PinnedKind.Channel,
                    login = login,
                    name = login,
                    subtitle = "Pinned chat",
                    isLive = false
                )
        }
}

private fun Channel.toPinnedItem(): PinnedItem =
    PinnedItem(
        kind = PinnedKind.Channel,
        login = login,
        name = displayName,
        subtitle = if (isLive) gameName ?: "Live now" else "Pinned chat",
        message = title,
        isLive = isLive,
        viewers = if (isLive) formatViewers(viewerCount) else null,
        imageUrl = profileImageUrl
    )

@Composable
private fun HomeBody(
    state: DiscoveryUiState,
    pinnedChannelLogins: List<String>,
    onJoinChannel: (String) -> Unit,
    onRemovePin: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    val pinnedItems = remember(
        state.followedLive,
        state.recommendedStreams,
        state.searchResults,
        pinnedChannelLogins
    ) {
        buildHomePinnedItems(state, pinnedChannelLogins)
    }
    val carouselChannels = (state.followedLive + state.recommendedStreams)
        .distinctBy { it.login }
        .take(10)
    val channelsByLogin = (state.followedLive + state.recommendedStreams + state.searchResults)
        .distinctBy { it.login.lowercase() }
        .associateBy { it.login.lowercase() }
    val heroChannel = pinnedChannelLogins
        .map { it.lowercase().removePrefix("#").trim() }
        .firstNotNullOfOrNull { login -> channelsByLogin[login]?.takeIf { it.isLive } }
    var selectedFilter by remember { mutableIntStateOf(0) }
    val visiblePinnedItems = remember(pinnedItems, selectedFilter) {
        when (selectedFilter) {
            1 -> pinnedItems.filter { it.isLive }
            2 -> pinnedItems.filterNot { it.isLive }
            else -> pinnedItems
        }
    }
    val liveCount = pinnedItems.count { it.isLive }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(liveCount = liveCount, savedCount = pinnedItems.size, onSearch = onSearch)
            HomeFilterPills(selectedFilter = selectedFilter, onFilterSelected = { selectedFilter = it })
            PullToRefreshHint(onRefresh = onRefresh)

            if (carouselChannels.isNotEmpty()) {
                SectionHeader(title = "Live carousel")
                LiveFollowingRow(channels = carouselChannels, onClick = onJoinChannel)
                Spacer(Modifier.height(6.dp))
            }

            if (heroChannel != null) {
                HomeHeroCard(channel = heroChannel, onJoinChannel = onJoinChannel)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pinned chats",
                    color = Twick.Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {}
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = "Recent", color = Twick.Ink3, fontSize = 12.sp)
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Twick.Ink3, modifier = Modifier.size(14.dp))
                }
            }

            if (pinnedItems.isEmpty()) {
                EmptyHomePinnedState(onSearch = onSearch)
            } else if (visiblePinnedItems.isEmpty()) {
                EmptyFilteredPinnedState()
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    visiblePinnedItems.forEachIndexed { index, item ->
                        PinnedRow(
                            item = item,
                            index = index,
                            onClick = { onJoinChannel(item.login) },
                            onRemovePin = { onRemovePin(item.login) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(160.dp))
        }

        AddPinFab(
            onClick = onSearch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 98.dp)
        )
    }
}

@Composable
private fun EmptyHomePinnedState(onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Twick.S2)
                .border(1.dp, Twick.Hairline, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Twick.Ink, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No pinned chats yet",
            color = Twick.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Open a chat from Browse or Search to pin it here.",
            color = Twick.Ink3,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Twick.Accent)
                .clickable(onClick = onSearch)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                text = "Find channels",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyFilteredPinnedState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nothing in this view yet",
            color = Twick.Ink3,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HomeHeader(liveCount: Int, savedCount: Int, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Twick.PurpleGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Column {
                Text(
                    text = "Pinned",
                    color = Twick.Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "● $liveCount live",
                        color = Twick.Live,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = " · ", color = Twick.Ink4, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$savedCount saved",
                        color = Twick.Ink3,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TopBarIconButton(icon = Icons.Filled.Search, onClick = onSearch)
        }
    }
}

@Composable
private fun HomeFilterPills(selectedFilter: Int, onFilterSelected: (Int) -> Unit) {
    val filters = listOf(
        "All" to null,
        "Live" to Twick.Live,
        "Offline" to null
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        items(filters.size) { index ->
            val (label, dot) = filters[index]
            SoftPill(
                text = label,
                active = selectedFilter == index,
                leadingDot = dot,
                onClick = { onFilterSelected(index) }
            )
        }
    }
}

@Composable
private fun SoftPill(text: String, active: Boolean = false, leadingDot: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Twick.Ink else Twick.S2)
            .border(1.dp, if (active) Color.Transparent else Twick.Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingDot != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(leadingDot)
            )
        }
        Text(
            text = text,
            color = if (active) Color.Black else Twick.Ink2,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun PullToRefreshHint(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRefresh)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Twick.Ink4, modifier = Modifier.size(13.dp))
        Text(
            text = "PULL TO REFRESH",
            color = Twick.Ink4,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomeHeroCard(channel: Channel?, onJoinChannel: (String) -> Unit) {
    val heroName = channel?.displayName ?: "pokimane"
    val heroTitle = channel?.title ?: "late night vods reaction + co-stream w/ friends"
    val heroGame = channel?.gameName ?: "Just Chatting"
    val viewers = channel?.viewerCount ?: 12_400

    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(thumbnailGradient(heroName))
            .border(1.dp, Twick.Hairline.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .clickable(enabled = channel != null) {
                if (channel != null) onJoinChannel(channel.login)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            if (channel?.thumbnailUrl != null) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "$heroName / $heroGame",
                color = Color.White.copy(alpha = 0.22f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LivePill(viewerText = formatViewers(viewers))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlatformDot(platform = "twitch", size = 7.dp)
                    Text("3:42:11", color = Twick.Ink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = Twick.Accent, modifier = Modifier.size(15.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }

        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarCircle(name = heroName, size = 40.dp, imageUrl = channel?.profileImageUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = heroName, color = Twick.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    text = heroTitle,
                    color = Twick.Ink2,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "184",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Twick.Accent)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
                Text("NEW", color = Twick.Ink3, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun PinnedRow(
    item: PinnedItem,
    index: Int,
    onClick: () -> Unit,
    onRemovePin: () -> Unit
) {
    val restShape = when (index % 4) {
        1 -> RoundedCornerShape(topStart = 24.dp, topEnd = 12.dp, bottomEnd = 24.dp, bottomStart = 12.dp)
        2 -> RoundedCornerShape(16.dp)
        3 -> RoundedCornerShape(topStart = 12.dp, topEnd = 24.dp, bottomEnd = 12.dp, bottomStart = 24.dp)
        else -> RoundedCornerShape(20.dp)
    }
    var offsetX by remember(item.login) { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val actionWidth = 88.dp
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val swipeProgress = (-offsetX / actionWidthPx).coerceIn(0f, 1f)

    Box(modifier = Modifier.height(72.dp)) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PinnedRemoveSwipeAction(
                progress = swipeProgress,
                actionWidth = actionWidth,
                onClick = onRemovePin
            )
        }
        PinnedRowInner(
            item = item,
            shape = restShape,
            pressed = false,
            onClick = onClick,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(item.login) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (-offsetX >= actionWidthPx) {
                                offsetX = 0f
                                onRemovePin()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-actionWidthPx, 0f)
                        }
                    )
                }
        )
    }
}

@Composable
private fun PinnedRemoveSwipeAction(progress: Float, actionWidth: Dp, onClick: () -> Unit) {
    val color = Twick.Ink3
    val armed = progress >= 0.995f
    Box(
        modifier = Modifier
            .width(actionWidth)
            .height(72.dp)
            .padding(start = 10.dp, end = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (armed) {
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Twick.S2)
                    .border(1.dp, Twick.Hairline, RoundedCornerShape(14.dp))
                    .clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PushPin, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .rotate(-35f)
                            .background(color)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Unpin",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        } else if (progress > 0.08f) {
            Row(
                modifier = Modifier.padding(end = 18.dp),
                horizontalArrangement = Arrangement.spacedBy((-9).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.18f + progress * 0.24f),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.28f + progress * 0.34f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PinnedRowInner(
    item: PinnedItem,
    shape: RoundedCornerShape,
    pressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(if (pressed) Twick.S2 else if (item.isLive) Twick.Live.copy(alpha = 0.04f) else Twick.S1)
            .border(1.dp, if (item.isLive) Twick.Live.copy(alpha = 0.15f) else Twick.Hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (item.isLive) {
                StoryRingAvatar(name = item.name, imageUrl = item.imageUrl, avatarSize = 48.dp, ringWidth = 2.dp, ringGap = 1.5.dp)
            } else {
                AvatarCircle(name = item.name, size = 52.dp, imageUrl = item.imageUrl)
            }
            item.platform?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-1).dp, y = 1.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Twick.Bg)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlatformDot(platform = it, size = 10.dp)
                }
            }
            if (!item.isLive && item.scheduled != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Twick.S3)
                        .border(2.dp, Twick.Bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Twick.Ink2, modifier = Modifier.size(10.dp))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.name,
                    color = Twick.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = if (item.kind == PinnedKind.Channel && item.isLive) "LIVE" else if (item.kind == PinnedKind.Chat) "CHAT" else "",
                    color = if (item.isLive) Twick.Live else Twick.Ink4,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (item.scheduled != null) {
                    Text(
                        text = item.scheduled,
                        color = Twick.Ink3,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Twick.S2)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = item.message ?: item.subtitle,
                color = if (item.isLive) Twick.Ink2 else Twick.Ink3,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.isLive && item.viewers != null) {
                Text(
                    text = item.viewers,
                    color = Twick.Live,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (item.unread > 0) {
                Text(
                    text = if (item.mention) "@${item.unread}" else item.unread.toString(),
                    color = if (item.mention) Color.White else Twick.Ink2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (item.mention) Twick.Accent else Twick.S3)
                        .border(1.dp, if (item.mention) Color.Transparent else Twick.Hairline, RoundedCornerShape(999.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                )
            } else {
                Spacer(Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun HomeNowPlayingBar(channel: Channel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        hsvColor(285f, 0.45f, 0.26f),
                        hsvColor(320f, 0.38f, 0.18f)
                    )
                )
            )
            .border(1.dp, Twick.Hairline.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(2.dp)
                    .background(Brush.linearGradient(listOf(Twick.Accent, Twick.Live)))
            )
        }
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                AvatarCircle(name = channel.displayName, size = 36.dp, imageUrl = channel.profileImageUrl)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Twick.Bg)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlatformDot(platform = "twitch", size = 9.dp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Twick.Live))
                    Text(
                        text = channel.displayName,
                        color = Twick.Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("AUDIO", color = Twick.Ink3, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = channel.title ?: channel.gameName ?: "@${channel.login}",
                    color = Twick.Ink3,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Close, contentDescription = null, tint = Twick.Ink3, modifier = Modifier.size(18.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Twick.Ink),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun AddPinFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Twick.Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        Text(
            text = "Add pin",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun YouBody() {
    PlaceholderBody(
        icon = Icons.Filled.Person,
        title = "You",
        message = "Your account and saved spaces will live here."
    )
}

@Composable
private fun PlaceholderBody(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Twick.S2)
                .border(1.dp, Twick.Hairline, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Twick.Ink,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = Twick.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = Twick.Ink3,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BrowseBody(state: DiscoveryUiState, onJoinChannel: (String) -> Unit) {
    if (state.recommendedStreams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No streams available", color = Twick.Ink3, fontSize = 14.sp)
        }
        return
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        val chunked = (state.followedLive + state.recommendedStreams)
            .distinctBy { it.login }
            .chunked(2)
        chunked.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { channel ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                            .clickable { onJoinChannel(channel.login) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(thumbnailGradient(channel.login))
                        ) {
                            if (channel.thumbnailUrl != null) {
                                AsyncImage(
                                    model = channel.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Twick.Live)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ViewerCountLabel(
                                    count = channel.viewerCount,
                                    fontSize = 9,
                                    iconSize = 10.dp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = channel.displayName,
                            color = Twick.Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (channel.gameName != null) {
                            Text(
                                text = channel.gameName,
                                color = Twick.Ink3,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DiscoveryTopBar(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Twick.PurpleGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                    )
                }
            }
            Text(
                text = "twick",
                color = Twick.Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row {
            TopBarIconButton(icon = Icons.Filled.Search, onClick = onSearch)
        }
    }
}

@Composable
private fun TopBarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Twick.Ink, modifier = Modifier.size(22.dp))
    }
}

private data class BottomDestination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun DiscoveryBottomBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        BottomDestination("Home", Icons.Filled.Home),
        BottomDestination("Browse", Icons.Filled.Explore),
        BottomDestination("You", Icons.Filled.Person)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Twick.S1.copy(alpha = 0.96f))
            .border(1.dp, Twick.Hairline, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { idx, destination ->
            val isActive = idx == activeTab
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(if (isActive) 18.dp else 16.dp))
                    .background(if (isActive) Twick.AccentSoft else Color.Transparent)
                    .clickable { onTabSelected(idx) }
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = if (isActive) Twick.Ink else Twick.Ink3,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = destination.label,
                    color = if (isActive) Twick.Ink else Twick.Ink3,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onActionClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Twick.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (action != null) {
            Text(
                text = action,
                color = Twick.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Composable
private fun LiveFollowingRow(channels: List<Channel>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        items(channels, key = { it.login }) { channel ->
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onClick(channel.login) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StoryRingAvatar(
                    name = channel.displayName,
                    imageUrl = channel.profileImageUrl,
                    avatarSize = 64.dp,
                    ringWidth = 2.5.dp,
                    ringGap = 2.dp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = channel.displayName,
                    color = Twick.Ink2,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ViewerCountLabel(
                    count = channel.viewerCount,
                    fontSize = 9,
                    iconSize = 10.dp
                )
            }
        }
    }
}

@Composable
private fun FollowingRow(channel: Channel, isLive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            AvatarCircle(name = channel.displayName, size = 44.dp, imageUrl = channel.profileImageUrl)
            if (isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Twick.Live)
                        .border(2.dp, Twick.Bg, CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.displayName,
                color = if (isLive) Twick.Ink else Twick.Ink3,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLive && channel.gameName != null) {
                Text(
                    text = channel.gameName,
                    color = Twick.Ink3,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!isLive) {
                Text(text = "Offline", color = Twick.Ink4, fontSize = 11.sp)
            }
        }
        if (isLive) {
            Column(horizontalAlignment = Alignment.End) {
                ViewerCountLabel(
                    count = channel.viewerCount,
                    fontSize = 11,
                    iconSize = 12.dp
                )
            }
        }
    }
}

@Composable
private fun StreamCard(channel: Channel, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(thumbnailGradient(channel.login))
        ) {
            if (channel.thumbnailUrl != null) {
                AsyncImage(
                    model = channel.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Twick.Live)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "● LIVE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                ViewerCountLabel(
                    count = channel.viewerCount,
                    fontSize = 10,
                    iconSize = 11.dp,
                    color = Color.White
                )
            }


            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Twick.Twitch)
                )
                Text(text = "TWITCH", color = Twick.Ink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarCircle(name = channel.displayName, size = 32.dp, imageUrl = channel.profileImageUrl)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.displayName.lowercase(),
                        color = Twick.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel.gameName != null) {
                        Text(
                            text = " · ${channel.gameName}",
                            color = Twick.Ink3,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                if (channel.title != null) {
                    Text(
                        text = channel.title,
                        color = Twick.Ink3,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = Twick.Ink3,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {}
            )
        }
    }
}

@Composable
private fun StoryRingAvatar(
    name: String,
    imageUrl: String?,
    avatarSize: Dp,
    ringWidth: Dp = 2.5.dp,
    ringGap: Dp = 2.dp
) {
    val ringBrush = Brush.sweepGradient(listOf(Twick.Live, Twick.Accent, Twick.Live))
    val bgColor = Twick.Bg
    val totalSize = avatarSize + (ringWidth + ringGap) * 2

    Box(
        modifier = Modifier
            .size(totalSize)
            .drawBehind {
                val stroke = ringWidth.toPx()
                val gap = ringGap.toPx()
                val radius = size.minDimension / 2f
                drawCircle(brush = ringBrush, radius = radius - stroke / 2f, style = Stroke(width = stroke))
                drawCircle(color = bgColor, radius = radius - stroke - gap / 2f, style = Stroke(width = gap * 2))
            },
        contentAlignment = Alignment.Center
    ) {
        AvatarCircle(name = name, size = avatarSize, imageUrl = imageUrl)
    }
}

@Composable
private fun LivePill(viewerText: String? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Twick.Live)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "● LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        if (viewerText != null) {
            Text(
                text = viewerText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PlatformDot(platform: String, size: Dp) {
    val color = when (platform.lowercase()) {
        "kick" -> Twick.Kick
        "youtube" -> Twick.YouTube
        else -> Twick.Twitch
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun AvatarCircle(name: String, size: Dp, imageUrl: String? = null) {
    val seed = name.hashCode()
    val hue = ((seed % 360) + 360) % 360
    val brush = Brush.linearGradient(
        colors = listOf(
            hsvColor(hue.toFloat(), 0.55f, 0.65f),
            hsvColor(((hue + 40) % 360).toFloat(), 0.55f, 0.40f)
        )
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun thumbnailGradient(seed: String): Brush {
    val hue = ((seed.hashCode() % 360) + 360) % 360
    return Brush.linearGradient(
        colors = listOf(
            hsvColor(hue.toFloat(), 0.40f, 0.35f),
            hsvColor(hue.toFloat(), 0.30f, 0.18f)
        )
    )
}

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val hh = hue / 60f
    val x = c * (1f - kotlin.math.abs((hh % 2f) - 1f))
    val (r1, g1, b1) = when {
        hh < 1f -> Triple(c, x, 0f)
        hh < 2f -> Triple(x, c, 0f)
        hh < 3f -> Triple(0f, c, x)
        hh < 4f -> Triple(0f, x, c)
        hh < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = value - c
    return Color(r1 + m, g1 + m, b1 + m)
}

@Composable
private fun ViewerCountLabel(
    count: Int,
    fontSize: Int,
    iconSize: Dp,
    color: Color = Twick.Live,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = formatViewers(count),
            color = color,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatViewers(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
    count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}K"
    else -> count.toString()
}
