package io.github.bengidev.opencore.speech.presenter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic

/** Top recording bar — status dot, live waveform, timer, and cancel action. */
@Composable
internal fun SpeechRecordingIndicatorView(
    elapsedDurationSeconds: Double,
    audioLevels: List<Float>,
    isVoiceActive: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val barCount = SpeechRecordingDisplayLogic.DEFAULT_BAR_COUNT
    val barHeights = SpeechRecordingDisplayLogic.waveformBarHeights(
        levels = audioLevels,
        barCount = barCount,
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isVoiceActive) 1f else 0.7f,
        label = "voiceActivityDot",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.35f else 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Voice recording in progress" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isVoiceActive) {
                        palette.accentPrimary.copy(alpha = dotAlpha)
                    } else {
                        palette.textTertiary.copy(alpha = 0.7f * dotAlpha)
                    },
                ),
        )

        SpeechWaveformBarsView(
            heights = barHeights,
            activeColor = palette.accentPrimary,
            idleColor = palette.textTertiary.copy(alpha = 0.45f),
            modifier = Modifier
                .weight(1f)
                .height(22.dp),
        )

        Text(
            text = SpeechRecordingDisplayLogic.formatElapsedDuration(elapsedDurationSeconds),
            style = HomeTheme.typography.chipLabel,
            color = palette.textSecondary,
            modifier = Modifier.semantics {
                contentDescription = "Recording duration"
            },
        )

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.55f else 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel,
                )
                .semantics { contentDescription = "Cancel voice input" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SpeechWaveformBarsView(
    heights: List<Float>,
    activeColor: androidx.compose.ui.graphics.Color,
    idleColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        heights.forEachIndexed { index, height ->
            key(index) {
                val animatedHeight by animateFloatAsState(
                    targetValue = height,
                    animationSpec = tween(durationMillis = 80),
                    label = "waveformBar",
                )
                val barColor = if (animatedHeight > 0.18f) activeColor else idleColor
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(maxOf(4.dp, (animatedHeight * 22f).dp))
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(barColor),
                )
            }
        }
    }
}
