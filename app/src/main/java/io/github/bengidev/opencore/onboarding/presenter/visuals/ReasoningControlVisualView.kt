package io.github.bengidev.opencore.onboarding.presenter.visuals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import io.github.bengidev.opencore.onboarding.presenter.components.ReasoningLevelSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Default reasoning level — matches iOS demo (BALANCED). */
const val DefaultReasoningLevel = 0.62f

/** iOS-matched reasoning control: ring, slider, presets, bar chart. */
@Composable
internal fun ReasoningControlVisualView(
    reasoningLevel: Float,
    onReasoningLevelChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    appeared: Boolean = true
) {
    val palette = OnboardingTheme.palette

    val levelName = when {
        reasoningLevel < 0.38f -> "FAST ANSWER"
        reasoningLevel < 0.76f -> "BALANCED PLAN"
        else -> "DEEP REASONING"
    }
    val percentage = (reasoningLevel * 100).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReasoningRing(
                value = reasoningLevel,
                label = "$percentage%",
                modifier = Modifier.size(76.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = levelName,
                    style = OnboardingTheme.typography.monoSm,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentPrimary
                )
                Text(
                    text = "Set thinking before run.",
                    style = OnboardingTheme.typography.monoXs,
                    color = palette.textTertiary,
                    maxLines = 2
                )
            }
        }

        ReasoningLevelSlider(
            value = reasoningLevel,
            onValueChange = onReasoningLevelChanged,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PresetButton("FAST", 0.22f, reasoningLevel, { onReasoningLevelChanged(0.22f) }, Modifier.weight(1f))
            PresetButton("BALANCED", 0.62f, reasoningLevel, { onReasoningLevelChanged(0.62f) }, Modifier.weight(1f))
            PresetButton("DEEP", 0.90f, reasoningLevel, { onReasoningLevelChanged(0.90f) }, Modifier.weight(1f))
        }

        ComputeBudgetChart(
            reasoningLevel = reasoningLevel,
            appeared = appeared,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )
    }
}

@Composable
private fun ReasoningRing(value: Float, label: String, modifier: Modifier = Modifier) {
    val palette = OnboardingTheme.palette
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "reasoning_ring"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val progressStroke = 5.dp.toPx()
            val trackStroke = 1.dp.toPx()
            val radius = size.minDimension / 2f - progressStroke / 2f
            drawCircle(
                color = palette.lineSoft.copy(alpha = 0.75f),
                radius = radius,
                style = Stroke(trackStroke)
            )
            drawArc(
                color = palette.accentPrimary,
                startAngle = -90f,
                sweepAngle = 360f * animatedValue,
                useCenter = false,
                style = Stroke(progressStroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = label,
            style = OnboardingTheme.typography.monoSm.copy(fontSize = 17.sp),
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun PresetButton(
    text: String,
    value: Float,
    currentValue: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val isSelected = abs(currentValue - value) < 0.08f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) palette.controlStrong else palette.surfaceSubtle.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (isSelected) palette.controlStrong.copy(alpha = 0.3f) else palette.lineSoft,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OnboardingTheme.typography.monoXs,
            color = if (isSelected) palette.controlStrongText else palette.textSecondary
        )
    }
}

@Composable
private fun ComputeBudgetChart(
    reasoningLevel: Float,
    appeared: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeights = listOf(12.dp, 16.dp, 20.dp, 24.dp, 28.dp, 32.dp, 36.dp, 40.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEachIndexed { index, height ->
            val normalizedIndex = (index + 1) / 8f
            val isActive = normalizedIndex <= reasoningLevel
            BudgetBar(
                height = height,
                isActive = isActive,
                index = index,
                appeared = appeared,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BudgetBar(
    height: Dp,
    isActive: Boolean,
    index: Int,
    appeared: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val barScaleY = remember { Animatable(0.35f) }
    val barAlpha = remember { Animatable(0f) }

    LaunchedEffect(appeared) {
        if (appeared) {
            delay(index * 35L)
            launch {
                barScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                barAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        } else {
            barScaleY.snapTo(0.35f)
            barAlpha.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                this.alpha = barAlpha.value
                scaleY = barScaleY.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (isActive) palette.accentPrimary
                else palette.textTertiary.copy(alpha = 0.22f)
            )
    )
}
