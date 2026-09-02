package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

private const val SweepDurationMs = 1_750
private const val SweepBandWidthRatio = 0.55f

@Composable
internal fun ShimmerText(
    text: String,
    baseColor: Color,
    isActive: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    val palette = OnboardingTheme.palette
    val highlightColor = if (palette.isDark) Color(0xFFF5F5F5) else Color.White
    val density = LocalDensity.current
    var textWidthPx by remember { mutableFloatStateOf(0f) }

    if (!isActive || reduceMotion) {
        Text(
            text = text,
            color = baseColor,
            style = style,
            modifier = modifier
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "ShimmerText")
    val shimmerOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SweepDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    val bandWidthPx = maxOf(textWidthPx * SweepBandWidthRatio, with(density) { 24.dp.toPx() })
    val startX = shimmerOffset * (textWidthPx + bandWidthPx)

    Text(
        text = text,
        modifier = modifier,
        onTextLayout = { layoutResult -> textWidthPx = layoutResult.size.width.toFloat() },
        style = style.copy(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to baseColor,
                    0.38f to lerp(baseColor, highlightColor.copy(alpha = 0.2f), 1f),
                    0.5f to highlightColor.copy(alpha = 0.55f),
                    0.62f to lerp(baseColor, highlightColor.copy(alpha = 0.2f), 1f),
                    1f to baseColor
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + bandWidthPx, 0f)
            )
        )
    )
}
