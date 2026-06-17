package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** iOS-matched reasoning slider — single canvas + absolute drag/tap positioning. */
@Composable
internal fun ReasoningLevelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val fraction = value.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val thumbWidthDp = 24.dp
    val thumbHeightDp = 20.dp
    val trackHeightDp = 5.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val trackHeightPx = with(density) { trackHeightDp.toPx() }
        val travelPx = with(density) { (maxWidth - thumbWidthDp).toPx() }

        fun fractionFromX(x: Float): Float {
            if (travelPx <= 0f) return fraction
            val centered = x - thumbWidthPx / 2f
            return (centered / travelPx).coerceIn(0f, 1f)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(travelPx, thumbWidthPx) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onValueChange(fractionFromX(change.position.x))
                    }
                }
                .pointerInput(travelPx, thumbWidthPx) {
                    detectTapGestures { offset ->
                        onValueChange(fractionFromX(offset.x))
                    }
                }
        ) {
            val centerY = size.height / 2f
            val trackTop = centerY - trackHeightPx / 2f
            val thumbOffset = travelPx * fraction
            val activeWidth = (thumbOffset + thumbWidthPx / 2f).coerceAtLeast(trackHeightPx)
            val trackRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            val thumbRadius = CornerRadius(with(density) { 10.dp.toPx() })

            drawRoundRect(
                color = palette.lineSoft.copy(alpha = 0.75f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeightPx),
                cornerRadius = trackRadius
            )
            drawRoundRect(
                color = palette.accentPrimary,
                topLeft = Offset(0f, trackTop),
                size = Size(activeWidth, trackHeightPx),
                cornerRadius = trackRadius
            )
            drawRoundRect(
                color = palette.surfaceRaised,
                topLeft = Offset(thumbOffset, centerY - thumbHeightPx / 2f),
                size = Size(thumbWidthPx, thumbHeightPx),
                cornerRadius = thumbRadius
            )
            drawRoundRect(
                color = palette.lineSoft,
                topLeft = Offset(thumbOffset, centerY - thumbHeightPx / 2f),
                size = Size(thumbWidthPx, thumbHeightPx),
                cornerRadius = thumbRadius,
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )
        }
    }
}
