package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.chatterinomobile.data.model.Badge
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.data.model.Paint as PaintModel

internal fun buildInlineContent(
    badges: List<Badge>,
    displayName: String,
    paint: PaintModel?,
    authorColor: Color,
    fragments: List<MessageFragment>
): Map<String, InlineTextContent> {
    val map = mutableMapOf<String, InlineTextContent>()

    badges.forEachIndexed { index, badge ->
        map["badge_$index"] = InlineTextContent(
            placeholder = Placeholder(
                width = 1.1.em,
                height = 1.1.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BadgeChip(badge = badge, size = 14.dp)
            }
        }
    }

    map["username"] = InlineTextContent(
        placeholder = Placeholder(
            width = nameWidth(displayName),
            height = 1.2.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
        )
    ) {
        PaintedUsername(
            name = displayName,
            fallbackColor = authorColor,
            paint = paint,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }

    fragments.forEachIndexed { index, fragment ->
        if (fragment is MessageFragment.Emote) {
            val ratio = 1.0f
            map["emote_$index"] = InlineTextContent(
                placeholder = Placeholder(
                    width = (1.6f * ratio).em,
                    height = 1.6.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
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

private fun nameWidth(name: String): TextUnit = (name.length.coerceAtLeast(1) * 0.6f).em
