package io.github.bengidev.opencore.chat.presenter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.utilities.ChatVoiceNotePlaybackDisplayLogic

@Composable
internal fun ChatWaveformBarsView(
    heights: List<Float>,
    progress: Double = 0.0,
    showsPlaybackProgress: Boolean = false,
    activeColor: Color,
    idleColor: Color,
    unplayedColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val barHeightScale = if (showsPlaybackProgress) 24f else 22f
    val dimmedColor = unplayedColor ?: idleColor
    val animatedProgress by animateFloatAsState(
        targetValue = if (showsPlaybackProgress) progress.toFloat() else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "waveformPlaybackProgress",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
    ) {
        val barSpacing = if (showsPlaybackProgress) 1.dp else 2.dp
        val barWidth = if (showsPlaybackProgress) {
            val totalSpacing = barSpacing * (heights.size - 1).coerceAtLeast(0)
            ((maxWidth - totalSpacing) / heights.size.coerceAtLeast(1)).coerceAtLeast(1.5.dp)
        } else {
            3.dp
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(barSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            heights.forEachIndexed { index, height ->
                val baseColor = if (height > 0.12f) activeColor else idleColor
                val color = if (!showsPlaybackProgress) {
                    baseColor
                } else {
                    val fill = ChatVoiceNotePlaybackDisplayLogic
                        .barPlaybackFill(index, heights.size, animatedProgress.toDouble())
                        .toFloat()
                    lerp(dimmedColor, baseColor, fill)
                }
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height((4.dp.value + barHeightScale * height).coerceAtLeast(4f).dp)
                        .background(
                            color = color,
                            shape = RoundedCornerShape(1.5.dp),
                        ),
                )
            }
        }

        if (showsPlaybackProgress && animatedProgress in 0.01f..0.99f) {
            val playheadWidth = 1.5.dp
            val density = LocalDensity.current
            val playheadOffset = with(density) {
                (maxWidth.toPx() * animatedProgress - playheadWidth.toPx() / 2f).toDp()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = playheadOffset)
                    .width(playheadWidth)
                    .fillMaxHeight(0.85f)
                    .background(
                        color = activeColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}
