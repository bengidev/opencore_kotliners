package io.github.bengidev.opencore.onboarding.presenter.visuals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.domain.OnboardingQueueItem
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay

private val QueueEnterSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 220f
)

private val QueueContentSizeSpec = tween<IntSize>(durationMillis = 420, easing = EaseOutCubic)

private val queueRowEnter = fadeIn(tween(420, easing = EaseOutCubic)) +
    expandVertically(tween(460, easing = EaseOutCubic)) +
    slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(460, easing = EaseOutCubic)
    )

private val queueRowExit = fadeOut(tween(280, easing = FastOutSlowInEasing)) +
    shrinkVertically(tween(320, easing = FastOutSlowInEasing))

/** Prompt queue demo — staggered rows, running pulse, animated timeline. */
@Composable
internal fun PromptQueueVisualView(
    queuedPromptCount: Int,
    modifier: Modifier = Modifier,
    appeared: Boolean = true
) {
    val visibleCount = queuedPromptCount.coerceIn(1, OnboardingQueueItem.samples.size)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = QueueContentSizeSpec),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingQueueItem.samples.forEachIndexed { index, item ->
            key(item.id) {
                AnimatedVisibility(
                    visible = appeared && index < visibleCount,
                    enter = queueRowEnter,
                    exit = queueRowExit
                ) {
                    QueueRow(
                        item = item,
                        index = index,
                        isLast = index == visibleCount - 1,
                        rowEnterDelayMs = index * 72L
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QueueRow(
    item: OnboardingQueueItem,
    index: Int,
    isLast: Boolean,
    rowEnterDelayMs: Long,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val radius = OnboardingTheme.radius
    val isRunning = item.status == OnboardingQueueItem.Status.RUNNING

    val statusColor = when (item.status) {
        OnboardingQueueItem.Status.RUNNING -> palette.accentPrimary
        OnboardingQueueItem.Status.NEXT -> palette.warning
        OnboardingQueueItem.Status.QUEUED -> palette.textSecondary
        OnboardingQueueItem.Status.READY -> palette.success
    }

    val contentAlpha = remember(item.id) { Animatable(0f) }
    val contentOffset = remember(item.id) { Animatable(14f) }

    LaunchedEffect(item.id, rowEnterDelayMs) {
        contentAlpha.snapTo(0f)
        contentOffset.snapTo(14f)
        delay(rowEnterDelayMs)
        contentAlpha.animateTo(1f, QueueEnterSpring)
        contentOffset.animateTo(0f, QueueEnterSpring)
    }

    val runningBorderAlpha by animateFloatAsState(
        targetValue = if (isRunning) 0.34f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "running_border_alpha"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isRunning -> palette.accentPrimary.copy(alpha = runningBorderAlpha.coerceAtLeast(0.34f))
            index == 0 -> palette.accentPrimary.copy(alpha = 0.34f)
            else -> palette.lineSoft
        },
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "queue_row_border"
    )

    val pulseTransition = rememberInfiniteTransition(label = "queue_running_pulse")
    val runningDotScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "running_dot_scale"
    )
    val runningDotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 0.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720),
            repeatMode = RepeatMode.Reverse
        ),
        label = "running_dot_alpha"
    )
    val runningCardAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isRunning) 0.62f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "running_card_alpha"
    )
    val hourglassRotation by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 180f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hourglass_rotation"
    )
    val connectorProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning && !isLast) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connector_progress"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha.value
                translationY = contentOffset.value
            }
            .clip(RoundedCornerShape(radius.sm))
            .background(
                palette.surfaceSubtle.copy(
                    alpha = if (isRunning) runningCardAlpha else if (index == 0) 0.5f else 0.3f
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(radius.sm)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.width(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .scale(runningDotScale)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = runningDotAlpha.coerceIn(0.45f, 1f)))
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(palette.lineSoft.copy(alpha = 0.72f))
                ) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp * connectorProgress)
                                .align(Alignment.TopCenter)
                                .background(palette.accentPrimary.copy(alpha = 0.55f))
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.status.name,
                    style = OnboardingTheme.typography.monoXs,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
                Text(
                    text = item.title,
                    style = OnboardingTheme.typography.monoSm,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = item.detail,
                style = OnboardingTheme.typography.monoXs,
                color = palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = if (isRunning) Icons.Filled.HourglassTop else Icons.AutoMirrored.Filled.List,
            contentDescription = item.status.name,
            tint = if (isRunning) palette.accentPrimary else palette.textTertiary,
            modifier = Modifier
                .size(16.dp)
                .rotate(if (isRunning) hourglassRotation else 0f)
        )
    }
}
