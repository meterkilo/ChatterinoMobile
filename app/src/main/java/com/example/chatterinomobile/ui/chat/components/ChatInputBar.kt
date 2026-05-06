package com.example.chatterinomobile.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatterinomobile.ui.theme.Twick

@Composable
fun ChatInputBar(
    enabled: Boolean,
    hint: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEmotePicker: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val canSend = enabled && text.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Twick.S1)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconCircle(
            enabled = enabled,
            onClick = { onEmotePicker?.invoke() }
        ) {
            Icon(
                imageVector = Icons.Outlined.SentimentSatisfied,
                contentDescription = "Emotes",
                tint = if (enabled) Twick.Ink2 else Twick.Ink4
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .background(Twick.S2, RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = if (focused) Twick.Accent else Twick.Hairline,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (enabled) text = it },
                enabled = enabled,
                singleLine = false,
                maxLines = 4,
                cursorBrush = SolidColor(Twick.Accent),
                textStyle = LocalTextStyle.current.copy(color = Twick.Ink, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            color = Twick.Ink3,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (canSend) Twick.Accent else Twick.S2,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(enabled = canSend) {
                    val toSend = text.trim()
                    if (toSend.isNotEmpty()) {
                        onSend(toSend)
                        text = ""
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (canSend) Color.White else Twick.Ink3
            )
        }
    }
}

@Composable
private fun IconCircle(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
