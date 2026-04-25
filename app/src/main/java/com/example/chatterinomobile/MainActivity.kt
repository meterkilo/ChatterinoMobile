package com.example.chatterinomobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.data.model.MessageType
import com.example.chatterinomobile.data.model.UserChatState
import com.example.chatterinomobile.data.repository.AuthRepository
import com.example.chatterinomobile.ui.auth.AuthUiState
import com.example.chatterinomobile.ui.auth.AuthViewModel
import com.example.chatterinomobile.ui.auth.TwitchLoginWebView
import com.example.chatterinomobile.ui.channels.ActiveChannelState
import com.example.chatterinomobile.ui.channels.ChannelTabsViewModel
import com.example.chatterinomobile.ui.chat.ChatUiState
import com.example.chatterinomobile.ui.chat.ChatViewModel
import com.example.chatterinomobile.ui.chat.describeParent
import com.example.chatterinomobile.ui.settings.SettingsUiState
import com.example.chatterinomobile.ui.settings.SettingsViewModel
import com.example.chatterinomobile.ui.theme.ChatterinoMobileTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModel()
    private val tabsViewModel: ChannelTabsViewModel by viewModel()
    private val chatViewModel: ChatViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authState by authViewModel.uiState.collectAsState()
            val activeChannel by tabsViewModel.activeChannel.collectAsState()
            val tabsError by tabsViewModel.errorMessage.collectAsState()
            val chatState by chatViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()

            // Single seam between the tabs VM and the chat VM. ChannelTabs
            // owns selection; Chat reacts to it. Keeping this in the screen
            // (rather than coupling the two VMs) keeps both unit-testable
            // in isolation.
            LaunchedEffect(activeChannel.channelLogin, activeChannel.hydration?.channelId) {
                chatViewModel.setActiveChannel(
                    channelLogin = activeChannel.channelLogin,
                    channelId = activeChannel.hydration?.channelId
                )
            }

            ChatterinoMobileTheme {
                val authorizeUrl = authState.authorizeUrl
                if (authorizeUrl != null) {
                    TwitchLoginWebView(
                        url = authorizeUrl,
                        redirectUri = AuthRepository.REDIRECT_URI,
                        onRedirect = authViewModel::onRedirectIntercepted,
                        onCancel = authViewModel::cancelLogin
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        DevScreen(
                            authState = authState,
                            activeChannel = activeChannel,
                            tabsError = tabsError,
                            chatState = chatState,
                            settingsState = settingsState,
                            onStartLogin = authViewModel::startLogin,
                            onLogout = authViewModel::logout,
                            onJoinChannel = tabsViewModel::joinChannel,
                            onSendMessage = chatViewModel::sendMessage,
                            onClearCache = settingsViewModel::clearCache,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevScreen(
    authState: AuthUiState,
    activeChannel: ActiveChannelState,
    tabsError: String?,
    chatState: ChatUiState,
    settingsState: SettingsUiState,
    onStartLogin: () -> Unit,
    onLogout: () -> Unit,
    onJoinChannel: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    var channelInput by rememberSaveable { mutableStateOf(activeChannel.channelLogin ?: "") }
    var messageInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Chatterino Mobile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        AuthCard(
            state = authState,
            onStartLogin = onStartLogin,
            onLogout = onLogout
        )

        CardBlock(title = "Channel") {
            OutlinedTextField(
                value = channelInput,
                onValueChange = { channelInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Channel login") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onJoinChannel(channelInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join Channel")
            }

            activeChannel.channelLogin?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Active channel: $it")
            }

            activeChannel.hydration?.let { hydration ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("Hydration: ")
                        append(
                            when {
                                hydration.isLoading -> "loading"
                                hydration.isReady -> "ready"
                                hydration.errorMessage != null -> "failed"
                                else -> "idle"
                            }
                        )
                    }
                )
                hydration.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            activeChannel.roomState?.let { roomState ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Room modes: slow=${roomState.slowModeSeconds}s, " +
                        "subs=${roomState.subscribersOnly}, " +
                        "emote-only=${roomState.emoteOnly}, " +
                        "followers=${roomState.followersOnlyMinutes ?: "off"}"
                )
            }

            activeChannel.userState?.let { UserStateSummary(it) }

            tabsError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        CardBlock(title = "Send") {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSendMessage(messageInput)
                    messageInput = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Message")
            }

            chatState.sendStatusMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            chatState.sendErrorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        CardBlock(title = "Messages") {
            when {
                chatState.isLoadingHistory -> Text(
                    text = "Loading scrollback…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                chatState.recentMessages.isEmpty() -> Text(
                    text = "No messages yet. Join a channel and wait for chat.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> chatState.recentMessages.takeLast(30).forEach { message ->
                    MessageRow(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        CardBlock(title = "Settings") {
            Button(
                onClick = onClearCache,
                enabled = !settingsState.isClearingCache,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (settingsState.isClearingCache) "Clearing…" else "Clear cache")
            }

            settingsState.statusMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            settingsState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AuthCard(
    state: AuthUiState,
    onStartLogin: () -> Unit,
    onLogout: () -> Unit
) {
    CardBlock(title = "Auth") {
        val status = when {
            state.isLoading -> "Checking session..."
            state.isAwaitingAuthorization -> "Opening Twitch login..."
            state.isLoggedIn -> "Signed in as ${state.login ?: state.userId ?: "unknown"}"
            else -> "Not signed in"
        }
        Text(status)

        if (state.isLoading || state.isAwaitingAuthorization) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }

        state.successMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoggedIn) {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }
        } else {
            Button(
                onClick = onStartLogin,
                enabled = !state.isLoading && !state.isAwaitingAuthorization,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In With Twitch")
            }
        }
    }
}

@Composable
private fun CardBlock(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun UserStateSummary(userState: UserChatState) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "You in chat: ${userState.displayName ?: userState.login ?: userState.userId ?: "unknown"}"
    )
    if (userState.badges.isNotEmpty()) {
        Text(
            "Badges: ${userState.badges.joinToString { it.description }}"
        )
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    val body = renderFragments(message.fragment)
    val systemText = (message.Type as? MessageType.System)?.text

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = message.author.displayName,
                fontWeight = FontWeight.SemiBold
            )
            if (message.reply != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "replying to ${message.reply.describeParent()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (systemText != null) {
            Text(
                text = systemText,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (body.isNotBlank()) {
            Text(body)
        }
    }
}

private fun renderFragments(fragments: List<MessageFragment>): String =
    buildString {
        for (fragment in fragments) {
            when (fragment) {
                is MessageFragment.Text -> append(fragment.content)
                is MessageFragment.Emote -> append(":${fragment.name}:")
                is MessageFragment.Mention -> append("@${fragment.username}")
            }
        }
    }
