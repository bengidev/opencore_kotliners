package io.github.bengidev.opencore.chat.presenter

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme

@Composable
internal fun ChatStreamingPulseDot(
    modifier: Modifier = Modifier,
) {
    val palette = ChatTheme.palette
    val transition = rememberInfiniteTransition(label = "streaming-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse-alpha",
    )

    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(palette.streamingDot.copy(alpha = alpha)),
    )
}
