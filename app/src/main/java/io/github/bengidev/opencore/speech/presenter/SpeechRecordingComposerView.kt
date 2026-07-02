package io.github.bengidev.opencore.speech.presenter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic

/** Full-width recording composer — replaces the text field while speech mode is active. */
@Composable
internal fun SpeechRecordingComposerView(
    elapsedDurationSeconds: Double,
    audioLevels: List<Float>,
    isVoiceActive: Boolean,
    isTranscribing: Boolean,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val shape = RoundedCornerShape(20.dp)
    val fill = palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.45f else 0.65f)
    val border = palette.textTertiary.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .height(56.dp)
            .semantics {
                contentDescription = if (isTranscribing) {
                    "Transcribing voice"
                } else {
                    "Voice recording in progress"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecordingIndicator(isVoiceActive = isVoiceActive, isTranscribing = isTranscribing)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        ) {
            val barCount = SpeechRecordingDisplayLogic.composerBarCount(
                forWidthDp = with(LocalDensity.current) { maxWidth.toPx() / density },
            )
            val barHeights = SpeechRecordingDisplayLogic.waveformBarHeights(
                levels = audioLevels,
                barCount = barCount,
            )
            SpeechComposerWaveformView(
                heights = barHeights,
                activeColor = palette.accentPrimary,
                idleColor = palette.textTertiary.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            )
        }

        Text(
            text = SpeechRecordingDisplayLogic.formatElapsedDuration(elapsedDurationSeconds),
            style = HomeTheme.typography.chipLabel,
            color = palette.textSecondary,
            modifier = Modifier.semantics { contentDescription = "Recording duration" },
        )

        if (isTranscribing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .semantics { contentDescription = "Transcribing voice" },
                strokeWidth = 2.dp,
                color = palette.accentPrimary,
            )
        } else if (onCancel != null) {
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
                    .semantics { contentDescription = "Cancel recording" },
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
}

@Composable
private fun RecordingIndicator(
    isVoiceActive: Boolean,
    isTranscribing: Boolean,
) {
    val palette = HomeTheme.palette

    Box(contentAlignment = Alignment.Center) {
        if (isTranscribing) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .border(2.dp, palette.accentPrimary.copy(alpha = 0.35f), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isTranscribing -> palette.textTertiary.copy(alpha = 0.5f)
                        isVoiceActive -> palette.accentPrimary
                        else -> palette.textTertiary.copy(alpha = 0.7f)
                    },
                ),
        )
    }
}

@Composable
private fun SpeechComposerWaveformView(
    heights: List<Float>,
    activeColor: androidx.compose.ui.graphics.Color,
    idleColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val barWidthPx = with(LocalDensity.current) { 2.5.dp.toPx() }
    val barSpacingPx = with(LocalDensity.current) { 2.dp.toPx() }
    val cornerRadiusPx = with(LocalDensity.current) { 1.5.dp.toPx() }

    Canvas(modifier = modifier) {
        if (heights.isEmpty()) return@Canvas
        val slotWidth = barWidthPx + barSpacingPx
        val totalWidth = heights.size * slotWidth - barSpacingPx
        val startX = ((size.width - totalWidth) / 2f).coerceAtLeast(0f)
        val maxHeight = size.height

        heights.forEachIndexed { index, height ->
            val barHeight = maxOf(4.dp.toPx(), height * maxHeight)
            val x = startX + index * slotWidth
            val y = (maxHeight - barHeight) / 2f
            val color = if (height > 0.12f) activeColor else idleColor
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }
    }
}
