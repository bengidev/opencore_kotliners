package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/**
 * Diagonal hatch pattern background.
 * Creates a fine technical texture that gives the UI a physical surface feel.
 */
@Composable
internal fun DiagonalHatchPattern(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    strokeWidth: Dp = 1.dp,
    color: Color = OnboardingTheme.palette.lineSoft.copy(alpha = if (OnboardingTheme.palette.isDark) 0.10f else 0.04f),
    angle: Float = 45f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = spacing.toPx()
        val strokeWidthPx = strokeWidth.toPx()
        val diagonal = size.width + size.height
        val step = spacingPx

        for (i in -diagonal.toInt()..diagonal.toInt() step step.toInt()) {
            drawLine(
                color = color,
                start = Offset(i.toFloat(), 0f),
                end = Offset(i.toFloat() - diagonal, diagonal),
                strokeWidth = strokeWidthPx
            )
        }
    }
}

/**
 * Pixel grid background with dot pattern.
 * Creates a subtle technical grid texture.
 */
@Composable
internal fun PixelGridBackground(
    modifier: Modifier = Modifier,
    spacing: Dp = 15.dp,
    dotSize: Dp = 1.dp,
    color: Color = OnboardingTheme.palette.textTertiary.copy(alpha = if (OnboardingTheme.palette.isDark) 0.06f else 0.04f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = spacing.toPx()
        val dotSizePx = dotSize.toPx()
        val radius = dotSizePx / 2f

        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(x, y)
                )
                x += spacingPx
            }
            y += spacingPx
        }
    }
}
