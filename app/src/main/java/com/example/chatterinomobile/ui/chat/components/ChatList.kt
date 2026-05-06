package com.example.chatterinomobile.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.ui.theme.Twick
import kotlinx.coroutines.launch

@Composable
fun ChatList(
    messages: List<ChatMessage>,
    deletedIds: Set<String>,
    showTimestamp: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 6.dp),
    currentUserLogin: String? = null
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val atBottom by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index >= layout.totalItemsCount - 2
        }
    }

    var pausedSinceCount by remember { mutableIntStateOf(messages.size) }

    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) {
            pausedSinceCount = 0
            return@LaunchedEffect
        }
        if (atBottom) {
            pausedSinceCount = messages.size
            state.scrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.Top
        ) {
            items(messages, key = { it.id }) { message ->
                val deleted = message.id in deletedIds
                val highlight = currentUserLogin != null &&
                    message.fragment.any { it is MessageFragment.Mention &&
                        it.username.equals(currentUserLogin, ignoreCase = true) }
                ChatMessageRow(
                    message = message,
                    showTimestamp = showTimestamp,
                    deleted = deleted,
                    highlight = highlight
                )
            }
        }

        val pendingCount = (messages.size - pausedSinceCount).coerceAtLeast(0)
        AnimatedVisibility(
            visible = !atBottom && pendingCount > 0,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            ResumePill(pending = pendingCount, onClick = {
                scope.launch {
                    state.scrollToItem(messages.lastIndex.coerceAtLeast(0))
                    pausedSinceCount = messages.size
                }
            })
        }
    }
}

@Composable
private fun ResumePill(pending: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Twick.Accent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = if (pending == 1) "1 new message" else "$pending new messages",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
