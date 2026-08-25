package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.launch

private val TrackHeight = 58.dp
private val KnobSize = 46.dp
private val InnerInset = 6.dp
private const val TrailingUnlockThreshold = 0.92f

@Composable
internal fun SwipeToStartView(
    isUnlocked: Boolean,
    reduceMotion: Boolean,
    onComplete: suspend () -> Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isCompleting by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val trackColor by animateColorAsState(
        targetValue = palette.surfaceSubtle,
        animationSpec = tween(280),
        label = "SwipeTrack"
    )
    val borderColor by animateColorAsState(
        targetValue = palette.lineSoft,
        animationSpec = tween(280),
        label = "SwipeBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = palette.textTertiary,
        animationSpec = tween(280),
        label = "SwipeLabel"
    )
    val knobColor by animateColorAsState(
        targetValue = palette.controlStrong,
        animationSpec = tween(280),
        label = "SwipeKnob"
    )
    val knobIconColor by animateColorAsState(
        targetValue = palette.controlStrongText,
        animationSpec = tween(280),
        label = "SwipeKnobIcon"
    )

    val knobScale by animateFloatAsState(
        targetValue = if (isDragging) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SwipeKnobScale"
    )

    val animatedDrag by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = when {
            reduceMotion -> spring(stiffness = Spring.StiffnessHigh)
            isCompleting -> spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
            else -> spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "SwipeDragOffset"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .semantics { contentDescription = "Swipe to get started" }
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val knobSizePx = with(density) { KnobSize.toPx() }
        val innerInsetPx = with(density) { InnerInset.toPx() }
        val maxDrag = (maxWidthPx - knobSizePx - innerInsetPx * 2).coerceAtLeast(1f)
        val labelOpacity = (1f - animatedDrag / maxDrag).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(percent = 50))
                .background(trackColor)
                .border(1.dp, borderColor, RoundedCornerShape(percent = 50))
                .pointerInput(isUnlocked, isCompleting) {
                    if (isUnlocked || isCompleting) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            isDragging = false
                            val reachedTrailing = dragOffsetPx >= maxDrag * TrailingUnlockThreshold
                            if (reachedTrailing) {
                                scope.launch {
                                    if (isUnlocked || isCompleting) return@launch
                                    isCompleting = true
                                    dragOffsetPx = maxDrag
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val succeeded = onComplete()
                                    if (!succeeded) {
                                        dragOffsetPx = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    isCompleting = false
                                }
                            } else {
                                dragOffsetPx = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetPx = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (isUnlocked || isCompleting) return@detectHorizontalDragGestures
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDrag)
                        }
                    )
                }
        ) {
            ShimmerText(
                text = "swipe to get started",
                baseColor = labelColor,
                isActive = !isUnlocked && labelOpacity > 0.15f,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = labelOpacity },
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            )

            Box(
                modifier = Modifier
                    .padding(start = InnerInset)
                    .offset(x = with(density) { animatedDrag.toDp() })
                    .size(KnobSize)
                    .scale(knobScale)
                    .clip(CircleShape)
                    .background(knobColor)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = knobIconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
