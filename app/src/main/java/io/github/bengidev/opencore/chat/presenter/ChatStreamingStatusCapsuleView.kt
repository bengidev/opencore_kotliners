package io.github.bengidev.opencore.chat.presenter

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.shared.ui.rememberReduceMotion

/** Compact status chip above the composer while a response is streaming. */
@Composable
internal fun ChatStreamingStatusCapsuleView(modifier: Modifier = Modifier) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    val reduceMotion = rememberReduceMotion()

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.assistantBubble)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "Processing, streaming" }
            .testTag("chat-streaming-status-capsule"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Processing",
            style = typography.streamingLabel,
            color = palette.reasoningText,
        )
        if (reduceMotion) {
            ProcessingDot(alpha = 1f)
        } else {
            val transition = rememberInfiniteTransition(label = "processing-capsule-dot")
            val dotAlpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "processing-dot-alpha",
            )
            ProcessingDot(alpha = dotAlpha)
        }
    }
}

@Composable
private fun ProcessingDot(alpha: Float) {
    val palette = ChatTheme.palette
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(palette.reasoningText.copy(alpha = alpha)),
    )
}
