package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import java.util.UUID

/** Collapsible reasoning card — mirrors iOS `ChatReasoningCardView`. */
@Composable
internal fun ChatReasoningCardView(
    messageId: UUID,
    content: String,
    isComplete: Boolean,
    isStreaming: Boolean,
    hasCompetingStream: Boolean = false,
    onCollapsed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    val expansionKey = messageId.toString()
    var isExpanded by rememberSaveable(expansionKey) { mutableStateOf(true) }
    var didAutoCollapse by remember(messageId) { mutableStateOf(false) }

    val showsBody = isStreaming || content.isNotEmpty()

    SideEffect {
        if (
            !didAutoCollapse &&
            ChatReasoningCollapsePolicy.shouldAutoCollapse(
                hasCompetingStream = hasCompetingStream,
                isThinkingStreaming = isStreaming,
            )
        ) {
            didAutoCollapse = true
            isExpanded = false
            onCollapsed()
        }
    }

    ChatMessageCardChrome(
        modifier = modifier
            .clickable(enabled = showsBody) {
                if (showsBody) isExpanded = !isExpanded
            }
            .testTag("chat-reasoning-card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = palette.streamingDot,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (isComplete) "Thought" else "Thinking",
                style = typography.reasoningHeader,
                color = palette.reasoningText,
            )
            if (isStreaming) {
                ChatStreamingPulseDot()
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showsBody) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.messageMetaText,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                )
            }
        }

        if (showsBody && isExpanded) {
            StreamingReasoningText(
                content = content,
                isStreaming = isStreaming,
            )
        }
        }
    }
}

@Composable
private fun StreamingReasoningText(
    content: String,
    isStreaming: Boolean,
) {
    val typography = ChatTheme.typography
    val displayedContent = content.ifEmpty { if (isStreaming) "…" else "" }

    ChatRichContentColumn(
        markdown = displayedContent,
        profile = ChatMarkwonRenderer.Profile.Thinking,
        modifier = Modifier.fillMaxWidth(),
        progressive = isStreaming,
        streamingRawTextStyle = typography.reasoningBody,
    )
}
