package io.github.bengidev.opencore.chat.presenter

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme

private val CardShape = RoundedCornerShape(14.dp)

/** Collapsible reasoning card — mirrors iOS `ChatReasoningCardView`. */
@Composable
internal fun ChatReasoningCardView(
    content: String,
    isComplete: Boolean,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    var isExpanded by remember(isStreaming) { mutableStateOf(isStreaming) }
    var didAutoCollapse by remember { mutableStateOf(false) }

    val showsBody = isStreaming || content.isNotEmpty()

    LaunchedEffect(isStreaming, showsBody) {
        if (!isStreaming && showsBody && !didAutoCollapse) {
            didAutoCollapse = true
            isExpanded = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.reasoningCard)
            .border(0.5.dp, palette.reasoningBorder, CardShape)
            .clickable(enabled = showsBody) {
                if (showsBody) isExpanded = !isExpanded
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("chat-reasoning-card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = palette.streamingDot,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isComplete) "Thought" else "Thinking",
                style = typography.reasoningHeader,
                color = palette.reasoningText
            )
            if (isStreaming) {
                ChatReasoningPulseDot()
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showsBody) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.messageMetaText,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }
        }

        if (showsBody && isExpanded) {
            StreamingReasoningText(
                content = content,
                isStreaming = isStreaming,
                textColor = palette.reasoningText,
                cursorColor = palette.streamingDot
            )
        }
    }
}

@Composable
private fun ChatReasoningPulseDot() {
    val palette = ChatTheme.palette
    val transition = rememberInfiniteTransition(label = "reasoning-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse-alpha"
    )

    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(palette.streamingDot.copy(alpha = alpha))
    )
}

@Composable
private fun StreamingReasoningText(
    content: String,
    isStreaming: Boolean,
    textColor: Color,
    cursorColor: Color
) {
    val typography = ChatTheme.typography
    val displayedContent = content.ifEmpty { if (isStreaming) "…" else "" }

    val cursorAlpha by if (isStreaming) {
        val transition = rememberInfiniteTransition(label = "reasoning-cursor")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
            label = "cursor-alpha"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    if (isStreaming) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            ChatStreamingTextView(
                text = displayedContent,
                textStyle = typography.reasoningBody,
                color = textColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "▍",
                style = typography.reasoningBody,
                color = cursorColor.copy(alpha = cursorAlpha),
            )
        }
    } else {
        Text(
            text = displayedContent,
            style = typography.reasoningBody,
            color = textColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
