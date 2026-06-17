package io.github.bengidev.opencore.onboarding.presenter.visuals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.domain.OnboardingPromptOption
import io.github.bengidev.opencore.onboarding.presenter.components.MonoChipButton
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay

/** iOS-matched idea studio: chips, then one bordered panel with prompt + response skeleton lines. */
@Composable
internal fun IdeaStudioVisualView(
    selectedPromptIndex: Int,
    onPromptSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    appeared: Boolean = true
) {
    val palette = OnboardingTheme.palette
    val radius = OnboardingTheme.radius

    val selectedPrompt = OnboardingPromptOption.samples.getOrElse(selectedPromptIndex) {
        OnboardingPromptOption.samples.first()
    }
    var typedLength by remember(selectedPrompt) { mutableIntStateOf(0) }
    var isTypingComplete by remember(selectedPrompt) { mutableStateOf(false) }

    val fullText = selectedPrompt.prompt

    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    LaunchedEffect(selectedPrompt) {
        isTypingComplete = false
        typedLength = 0
        if (reduceMotion) {
            typedLength = fullText.length
            isTypingComplete = true
        } else {
            fullText.forEachIndexed { index, _ ->
                delay(16L)
                typedLength = index + 1
            }
            isTypingComplete = true
        }
    }

    val visibleText = fullText.substring(0, typedLength)
    val cursorOpacity = if (!isTypingComplete) cursorAlpha else 0.34f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OnboardingPromptOption.samples.forEachIndexed { index, option ->
                MonoChipButton(
                    text = option.label,
                    selected = index == selectedPromptIndex,
                    onClick = { onPromptSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.sm))
                .background(
                    palette.surfaceBase.copy(alpha = if (palette.isDark) 0.5f else 0.15f)
                )
                .border(1.dp, palette.lineSoft, RoundedCornerShape(radius.sm))
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ">",
                    style = OnboardingTheme.typography.monoSm.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    color = palette.accentPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = visibleText,
                    style = OnboardingTheme.typography.monoSm,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(6.dp)
                        .height(15.dp)
                        .background(palette.textPrimary.copy(alpha = cursorOpacity))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(width = 0.84f, delayMillis = 0, active = appeared, reduceMotion = reduceMotion)
                SkeletonLine(width = 0.62f, delayMillis = 70, active = appeared, reduceMotion = reduceMotion)
                SkeletonLine(width = 0.74f, delayMillis = 140, active = appeared, reduceMotion = reduceMotion)
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    width: Float,
    delayMillis: Int,
    active: Boolean,
    reduceMotion: Boolean
) {
    val palette = OnboardingTheme.palette
    val alpha = remember { Animatable(0f) }
    val offsetX = remember { Animatable(-12f) }

    LaunchedEffect(active, reduceMotion) {
        if (!active) {
            alpha.snapTo(0f)
            offsetX.snapTo(-12f)
            return@LaunchedEffect
        }
        if (reduceMotion) {
            alpha.snapTo(1f)
            offsetX.snapTo(0f)
        } else {
            delay(delayMillis.toLong())
            alpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
            offsetX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(8.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                translationX = offsetX.value
            }
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        palette.accentPrimary.copy(alpha = 0.18f),
                        palette.textPrimary.copy(alpha = 0.20f),
                        palette.textTertiary.copy(alpha = 0.12f)
                    )
                )
            )
    )
}
