package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.data.model.MessageType
import com.example.chatterinomobile.data.model.ReplyMetadata
import com.example.chatterinomobile.ui.theme.Twick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val USERNAME_ID = "username"

private val MessageBodyStyle: TextStyle
    get() = TextStyle(
        color = Twick.Ink,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

private val TimestampStyle: TextStyle
    get() = TextStyle(
        color = Twick.Ink4,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

private val ReplyStyle: TextStyle
    get() = TextStyle(
        color = Twick.Ink3,
        fontSize = 11.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

private fun badgeId(index: Int): String = "badge_$index"

private fun emoteId(index: Int): String = "emote_$index"

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

private fun parseHexColor(hex: String): Color? {
    val normalized = hex.trim().removePrefix("#")
    if (normalized.length != 6 && normalized.length != 8) return null

    return try {
        val colorValue = normalized.toLong(radix = 16)
        if (normalized.length == 6) {
            Color(0xFF000000 or colorValue)
        } else {
            Color(colorValue)
        }
    } catch (_: NumberFormatException) {
        null
    }
}

private fun deterministicColor(login: String): Color {
    val palette = longArrayOf(
        0xFFFF4A80, 0xFFFFB13D, 0xFFFFD24A, 0xFF65E085,
        0xFF53D7D7, 0xFF6CA8FF, 0xFFB888FF, 0xFFFF87BA
    )
    val index = (login.hashCode() and Int.MAX_VALUE) % palette.size
    return Color(palette[index])
}

@Composable
fun ChatMessageRow(
    message: ChatMessage,
    showTimestamp: Boolean,
    deleted: Boolean,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val type = message.Type
    if (type is MessageType.System) {
        SystemMessageRow(text = type.text, timestamp = message.timestamp, showTimestamp = showTimestamp, modifier = modifier)
        return
    }

    val bg = if (highlight) Twick.AccentSoft else Color.Transparent
    val leftBorder = if (highlight) Twick.Accent else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(start = if (highlight) 0.dp else 10.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
            .alpha(if (deleted) 0.45f else 1f),
        verticalAlignment = Alignment.Top
    ) {
        if (highlight) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(leftBorder)
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val reply = message.reply
            if (reply != null) {
                ReplyPreviewRow(reply)
            }
            MessageBody(
                message = message,
                showTimestamp = showTimestamp,
                deleted = deleted
            )
        }
    }
}

@Composable
private fun MessageBody(
    message: ChatMessage,
    showTimestamp: Boolean,
    deleted: Boolean
) {
    val author = message.author
    val authorColor = remember(author.color, author.login) {
        val parsedColor = if (author.color != null) parseHexColor(author.color) else null
        parsedColor ?: deterministicColor(author.login)
    }

    val text = buildAnnotatedString {
        if (showTimestamp) {
            withStyle(TimestampStyle.toSpanStyle()) {
                append(formatTime(message.timestamp))
            }
            append("  ")
        }

        message.badges.forEachIndexed { index, _ ->
            appendInlineContent(badgeId(index), "·")
            append(" ")
        }

        appendInlineContent(USERNAME_ID, author.displayName)

        if (message.Type !is MessageType.Action) {
            withStyle(SpanStyle(color = Twick.Ink3)) { append(": ") }
        } else {
            append(" ")
        }

        if (deleted) {
            withStyle(SpanStyle(color = Twick.Ink3, fontStyle = FontStyle.Italic)) {
                append("<message deleted>")
            }
        } else {
            renderFragments(
                fragments = message.fragment,
                actionColor = if (message.Type is MessageType.Action) authorColor else null
            )
        }
    }

    val inline = remember(message.id, message.badges, author.id, author.paint) {
        buildInlineContent(
            badges = message.badges,
            displayName = author.displayName,
            paint = author.paint,
            authorColor = authorColor,
            fragments = message.fragment
        )
    }

    Text(
        text = text,
        style = MessageBodyStyle,
        inlineContent = inline,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReplyPreviewRow(reply: ReplyMetadata) {
    val parent = reply.parentDisplayName ?: reply.parentUserLogin ?: "—"
    val body = reply.parentBody ?: ""
    Text(
        text = "↪ @$parent: $body",
        style = ReplyStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, bottom = 2.dp)
    )
}

@Composable
private fun SystemMessageRow(
    text: String,
    timestamp: Long,
    showTimestamp: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTimestamp) {
            Text(formatTime(timestamp), style = TimestampStyle)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            "·",
            style = MaterialTheme.typography.bodySmall,
            color = Twick.Accent,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            color = Twick.Ink3,
            style = MessageBodyStyle.copy(fontSize = 12.sp)
        )
    }
}

private fun AnnotatedString.Builder.renderFragments(
    fragments: List<MessageFragment>,
    actionColor: Color?
) {
    fragments.forEachIndexed { index, fragment ->
        when (fragment) {
            is MessageFragment.Text -> {
                if (actionColor != null) {
                    withStyle(SpanStyle(color = actionColor, fontStyle = FontStyle.Italic)) {
                        append(fragment.content)
                    }
                } else {
                    append(fragment.content)
                }
            }
            is MessageFragment.Emote -> {
                appendInlineContent(emoteId(index), fragment.name)
            }
            is MessageFragment.Mention -> {
                withStyle(
                    SpanStyle(
                        color = Twick.Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("@${fragment.username}")
                }
            }
        }
    }
}
