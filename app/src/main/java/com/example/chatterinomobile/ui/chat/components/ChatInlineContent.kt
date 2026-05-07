package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.ui.theme.Twick
import com.example.chatterinomobile.data.model.Paint as PaintModel

@Composable
internal fun buildInlineContent(
    badges: List<Badge>,
    displayName: String,
    usernameSuffix: String,
    paint: PaintModel?,
    authorColor: Color,
    fragments: List<MessageFragment>
): Map<String, InlineTextContent> {
    val map = mutableMapOf<String, InlineTextContent>()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val usernameStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 18.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val suffixStyle = TextStyle(
        color = Twick.Ink3,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val usernameWidth = remember(displayName, usernameSuffix, usernameStyle, suffixStyle, density) {
        val usernameSize = textMeasurer.measure(displayName, usernameStyle).size.width
        val suffixSize = textMeasurer.measure(usernameSuffix, suffixStyle).size.width
        with(density) { (usernameSize + suffixSize).toSp() }
    }

    badges.forEachIndexed { index, badge ->
        if (badge.imageURL.isBlank()) return@forEachIndexed

        map["badge_$index"] = InlineTextContent(
            placeholder = Placeholder(
                width = 1.1.em,
                height = 1.1.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BadgeChip(badge = badge, size = 14.dp)
            }
        }
    }

    map["username"] = InlineTextContent(
        placeholder = Placeholder(
            width = usernameWidth,
            height = 1.2.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            PaintedUsername(
                name = displayName,
                fallbackColor = authorColor,
                paint = paint,
                style = usernameStyle
            )
            Text(text = usernameSuffix, style = suffixStyle)
        }
    }

    fragments.forEachIndexed { index, fragment ->
        if (fragment is MessageFragment.Emote) {
            val ratio = fragment.aspectRatio
                ?.takeIf { it.isFinite() && it > 0f }
                ?.coerceIn(0.4f, 4f)
                ?: 1.0f
            map["emote_$index"] = InlineTextContent(
                placeholder = Placeholder(
                    width = (1.6f * ratio).em,
                    height = 1.6.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                FragmentEmoteSlot(fragment)
            }
        }
    }

    return map
}

@Composable
private fun FragmentEmoteSlot(fragment: MessageFragment.Emote) {
    coil.compose.AsyncImage(
        model = fragment.url,
        contentDescription = fragment.name,
        modifier = Modifier.fillMaxSize()
    )
}
