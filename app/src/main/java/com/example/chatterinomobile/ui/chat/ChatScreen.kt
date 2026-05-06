package com.example.chatterinomobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterinomobile.data.model.RoomState
import com.example.chatterinomobile.ui.channels.ActiveChannelState
import com.example.chatterinomobile.ui.channels.ChannelTabsViewModel
import com.example.chatterinomobile.ui.chat.components.ChatInputBar
import com.example.chatterinomobile.ui.chat.components.ChatList
import com.example.chatterinomobile.ui.theme.Twick

@Composable
fun ChatRoute(
    chatViewModel: ChatViewModel,
    tabsViewModel: ChannelTabsViewModel,
    onBack: () -> Unit
) {
    val chatState by chatViewModel.uiState.collectAsState()
    val activeChannel by tabsViewModel.activeChannel.collectAsState()

    ChatScreen(
        state = chatState,
        activeChannel = activeChannel,
        onSend = chatViewModel::sendMessage,
        onBack = onBack
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    activeChannel: ActiveChannelState,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    val canSend = activeChannel.channelLogin != null && activeChannel.userState?.userId != null
    val hint = composeHint(activeChannel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Twick.Bg)
    ) {
        VideoPlaceholder()
        StreamerMetaRow(login = activeChannel.channelLogin, onBack = onBack)
        TabStrip()

        Box(modifier = Modifier.weight(1f)) {
            ChatList(
                messages = state.recentMessages,
                deletedIds = state.deletedIds,
                showTimestamp = false,
                modifier = Modifier.fillMaxSize(),
                currentUserLogin = activeChannel.userState?.login
            )
        }

        ChatInputBar(
            enabled = canSend,
            hint = hint,
            onSend = onSend
        )
    }
}

@Composable
private fun VideoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2A1F3D), Color(0xFF120A1F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "stream",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StreamerMetaRow(
    login: String?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Twick.Ink2
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Twick.Accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = login?.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = login ?: "channel",
                color = Twick.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "live",
                color = Twick.Ink3,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TabStrip() {
    val tabs = listOf("Chat", "About", "Clips", "Mods")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isActive = index == 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 18.dp)
            ) {
                Text(
                    text = label,
                    color = if (isActive) Twick.Ink else Twick.Ink3,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(28.dp)
                        .background(if (isActive) Twick.Accent else Color.Transparent)
                )
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

private fun composeHint(active: ActiveChannelState): String {
    val login = active.channelLogin ?: return "Join a channel"
    val constraint = describeRoomConstraint(active.roomState)
    if (constraint != null) return constraint
    if (active.userState?.userId == null) return "Sign in to chat in $login"
    return "Send a message"
}

private fun describeRoomConstraint(room: RoomState?): String? {
    if (room == null) return null
    if (room.subscribersOnly) return "Subscribers only"
    if (room.emoteOnly) return "Emotes only"
    if (room.followersOnlyMinutes != null) return "Followers only"
    if (room.slowModeSeconds > 0) return "Slow mode (${room.slowModeSeconds}s)"
    return null
}
