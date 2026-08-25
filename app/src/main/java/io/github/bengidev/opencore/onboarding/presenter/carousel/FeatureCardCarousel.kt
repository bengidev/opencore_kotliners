package io.github.bengidev.opencore.onboarding.presenter.carousel

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeature
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CardWidthRatio = 0.74f
private const val CardHeightRatio = 0.84f
private const val CardAspectRatio = 1.48f
private const val CardInterCardGapDp = 14f
private const val HoldDurationMs = 4_400L
private const val InitialAdvanceDelayMs = 680L
private const val RevealHoldDurationMs = 380L
private const val PressStartDelayMs = 64L
private const val CarouselSettleDelayMs = 920L
private const val PressMaximumDistanceDp = 14f
private const val CarouselDragMinimumDistanceDp = 8f
private val CarouselSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 340f)
private val RevealSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 203f)
private val RevealDismissSpring = spring<Float>(dampingRatio = 0.88f, stiffness = 502f)
private val AmbientBobEaseIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val AmbientBobEaseOut = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private const val AmbientBobRampInMs = 594
private const val AmbientBobRampOutMs = 396

@Composable
internal fun FeatureCardCarousel(
    isActive: Boolean,
    reduceMotion: Boolean,
    contentRevealed: Boolean = true,
    modifier: Modifier = Modifier,
    initialAdvanceDelayMs: Long = InitialAdvanceDelayMs
) {
    val features = remember { OnboardingFeature.catalog }
    var scrollTarget by remember { mutableFloatStateOf(0f) }
    var dragScroll by remember { mutableFloatStateOf(0f) }
    var releaseFrom by remember { mutableFloatStateOf(Float.NaN) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragOriginScrollIndex by remember { mutableFloatStateOf(0f) }
    var isImageRevealed by remember { mutableStateOf(false) }
    var isUserPressing by remember { mutableStateOf(false) }
    var isScrollAnimating by remember { mutableStateOf(false) }
    var isCarouselSettling by remember { mutableStateOf(false) }
    var pendingCarouselSettle by remember { mutableStateOf(false) }
    val focusedPressCharge = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scrollOffset = remember { Animatable(0f) }

    LaunchedEffect(scrollTarget, isUserDragging) {
        if (isUserDragging) {
            isCarouselSettling = false
            return@LaunchedEffect
        }
        if (!releaseFrom.isNaN()) {
            scrollOffset.snapTo(releaseFrom)
            releaseFrom = Float.NaN
        }
        isScrollAnimating = true
        scrollOffset.animateTo(scrollTarget, CarouselSpring)
        val normalized = normalizeScrollIndex(scrollTarget, features.size)
        if (abs(normalized - scrollTarget) > 0.001f) {
            scrollTarget = normalized
            scrollOffset.snapTo(normalized)
        }
        if (pendingCarouselSettle) {
            isCarouselSettling = true
            delay(CarouselSettleDelayMs)
            isCarouselSettling = false
            pendingCarouselSettle = false
        }
        isScrollAnimating = false
    }

    // Keep showing the finger position until the spring coroutine snaps scrollOffset — avoids a
    // one-frame jump back to the pre-drag scrollOffset when isUserDragging flips false.
    val displayScrollIndex = when {
        isUserDragging -> dragScroll
        !releaseFrom.isNaN() -> releaseFrom
        else -> scrollOffset.value
    }
    val isCarouselMoving = isUserDragging || isScrollAnimating || isCarouselSettling || scrollOffset.isRunning

    fun resetFocusedPress() {
        isUserPressing = false
        scope.launch {
            focusedPressCharge.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 500f))
        }
    }

    fun beginFocusedPress() {
        if (isUserPressing) return
        isUserPressing = true
        scope.launch {
            focusedPressCharge.animateTo(
                1f,
                tween(
                    durationMillis = 380,
                    easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                )
            )
        }
    }

    fun setUserPressing(pressing: Boolean) {
        if (pressing == isUserPressing) return
        isUserPressing = pressing
    }

    fun setImageRevealed(revealed: Boolean) {
        if (revealed == isImageRevealed) return
        if (revealed && isCarouselMoving) return
        isImageRevealed = revealed
    }

    suspend fun waitForScrollToSettle() {
        if (!scrollOffset.isRunning && !isScrollAnimating) return
        snapshotFlow { scrollOffset.isRunning || isScrollAnimating }
            .filter { !it }
            .first()
    }

    LaunchedEffect(isActive, reduceMotion, contentRevealed) {
        if (!isActive || !contentRevealed || reduceMotion || features.size <= 1) return@LaunchedEffect
        delay(initialAdvanceDelayMs)
        while (isActive && contentRevealed) {
            if (isUserDragging || isImageRevealed || isUserPressing) {
                delay(200)
                continue
            }
            isScrollAnimating = true
            scrollTarget += 1f
            pendingCarouselSettle = true
            waitForScrollToSettle()
            delay(HoldDurationMs)
        }
    }

    LaunchedEffect(isImageRevealed) {
        if (isImageRevealed) resetFocusedPress()
    }

    LaunchedEffect(isCarouselMoving) {
        if (!isCarouselMoving) return@LaunchedEffect
        if (isUserPressing) resetFocusedPress()
        if (isImageRevealed) setImageRevealed(false)
    }

    if (reduceMotion) {
        StaticFocusedCard(
            feature = features[wrappedIndex(scrollTarget.roundToInt(), features.size)],
            isImageRevealed = isImageRevealed,
            onRevealChanged = { setImageRevealed(it) },
            onPressChanged = { setUserPressing(it) },
            modifier = modifier
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                val feature = features[wrappedIndex(displayScrollIndex.roundToInt(), features.size)]
                contentDescription = "${feature.title}. ${feature.subtitle}"
            }
    ) {
        val cardSize = cardDimensions(maxWidth, maxHeight)
        val cardWidthPx = with(density) { cardSize.width.toPx() }
        val cardHeightPx = with(density) { cardSize.height.toPx() }
        val cardStepPx = cardWidthPx + with(density) { CardInterCardGapDp.dp.toPx() }
        val dragMinimumDistancePx = with(density) { CarouselDragMinimumDistanceDp.dp.toPx() }
        val pressMaximumDistancePx = with(density) { PressMaximumDistanceDp.dp.toPx() }
        val visibleIndices = visibleFeatureIndices(displayScrollIndex, features.size, isCarouselMoving)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cardStepPx, features.size, dragMinimumDistancePx, pressMaximumDistancePx, cardWidthPx, cardHeightPx) {
                    val velocityTracker = VelocityTracker()
                    var totalDragPx = 0f
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val start = down.position
                        var dragStarted = false
                        var pressStarted = false
                        var cancelled = false

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val downOnFocusedCard = abs(start.x - centerX) <= cardWidthPx / 2f &&
                            abs(start.y - centerY) <= cardHeightPx / 2f

                        val holdJob = scope.launch {
                            if (!downOnFocusedCard) return@launch
                            delay(PressStartDelayMs)
                            if (cancelled || dragStarted) return@launch
                            pressStarted = true
                            beginFocusedPress()
                            delay(RevealHoldDurationMs)
                            if (!cancelled && !dragStarted) {
                                setImageRevealed(true)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                holdJob.cancel()
                                if (dragStarted) {
                                    val anchor = dragOriginScrollIndex
                                    val flickDistance = velocityTracker.calculateVelocity().x * 0.22f
                                    val projected = dragScroll - flickDistance / cardStepPx
                                    val target = projected
                                        .roundToInt()
                                        .toFloat()
                                        .coerceIn(anchor.roundToInt() - 1f, anchor.roundToInt() + 1f)
                                    releaseFrom = dragScroll
                                    scrollTarget = target
                                    isUserDragging = false
                                    pendingCarouselSettle = true
                                } else {
                                    resetFocusedPress()
                                    setImageRevealed(false)
                                }
                                break
                            }

                            val distance = hypot(
                                change.position.x - start.x,
                                change.position.y - start.y
                            )

                            if (!dragStarted && !isImageRevealed) {
                                if (distance >= dragMinimumDistancePx) {
                                    holdJob.cancel()
                                    if (pressStarted) {
                                        resetFocusedPress()
                                    }
                                    dragStarted = true
                                    totalDragPx = 0f
                                    velocityTracker.resetTracking()
                                    val currentScroll = scrollOffset.value
                                    val normalized = normalizeScrollIndex(currentScroll, features.size)
                                    dragScroll = normalized
                                    dragOriginScrollIndex = normalized
                                    scrollTarget = normalized
                                    pendingCarouselSettle = false
                                    isCarouselSettling = false
                                    isUserDragging = true
                                    isScrollAnimating = true
                                } else if (distance > pressMaximumDistancePx) {
                                    cancelled = true
                                    holdJob.cancel()
                                    if (pressStarted) {
                                        resetFocusedPress()
                                    }
                                    break
                                }
                            }

                            if (!dragStarted) continue

                            val delta = change.position.x - change.previousPosition.x
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            totalDragPx += delta
                            val anchor = dragOriginScrollIndex
                            dragScroll = (anchor - totalDragPx / cardStepPx)
                                .coerceIn(anchor - 1f, anchor + 1f)
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            visibleIndices.forEach { index ->
                val relative = modularRelative(index, displayScrollIndex, features.size)
                val isFocused = abs(relative) < 0.05f
                val isRevealed = isFocused && isImageRevealed
                val shouldAmbientBob = isFocused && !isRevealed && !isUserDragging && !isCarouselMoving && !isUserPressing

                val baseScale = 1f - minOf(abs(relative), 1f) * 0.08f
                val targetScale = if (isRevealed) 1.05f else baseScale
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = when {
                        isRevealed -> RevealSpring
                        else -> RevealDismissSpring
                    },
                    label = "CardRevealScale"
                )
                val scale = if (isCarouselMoving) targetScale else animatedScale

                val blurRadius = if (isRevealed) {
                    0.dp
                } else {
                    val distance = (abs(relative) - 0.12f).coerceAtLeast(0f) / 0.88f
                    (distance.pow(2) * 2.2f * 4).dp
                }

                val opacity = 1f - minOf(abs(relative), 1f) * 0.28f
                val offsetXPx = with(density) { cardStepOffset(relative, cardSize.width).toPx() }

                FeatureCarouselCard(
                    feature = features[index],
                    cardWidth = cardSize.width,
                    cardHeight = cardSize.height,
                    relativePosition = relative,
                    isRevealed = isRevealed,
                    shouldAmbientBob = shouldAmbientBob,
                    pressCharge = if (isFocused) focusedPressCharge.value else 0f,
                    modifier = Modifier
                        .width(cardSize.width)
                        .height(cardSize.height)
                        .graphicsLayer {
                            translationX = offsetXPx
                            scaleX = scale
                            scaleY = scale
                            alpha = opacity
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
                                Modifier.blur(blurRadius)
                            } else {
                                Modifier
                            }
                        )
                        .zIndex(if (isRevealed) 100f else 10f - abs(relative) * 5f)
                )
            }
        }
    }
}

@Composable
private fun StaticFocusedCard(
    feature: OnboardingFeature,
    isImageRevealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardSize = cardDimensions(maxWidth, maxHeight)
        FeatureCarouselCard(
            feature = feature,
            cardWidth = cardSize.width,
            cardHeight = cardSize.height,
            relativePosition = 0f,
            isRevealed = isImageRevealed,
            shouldAmbientBob = false,
            pressCharge = 0f,
            modifier = Modifier.width(cardSize.width).height(cardSize.height)
        )
    }
}

@Composable
private fun FeatureCarouselCard(
    feature: OnboardingFeature,
    cardWidth: Dp,
    cardHeight: Dp,
    relativePosition: Float,
    isRevealed: Boolean,
    shouldAmbientBob: Boolean,
    pressCharge: Float,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val layout = remember(cardWidth, cardHeight) { CardLayoutMetrics(cardWidth, cardHeight) }
    var ambientOffsetX by remember { mutableFloatStateOf(0f) }
    var ambientOffsetY by remember { mutableFloatStateOf(0f) }
    val bobAmplitude = remember { Animatable(0f) }

    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = if (isRevealed) RevealSpring else RevealDismissSpring,
        label = "CardReveal"
    )

    val cardBackground by animateColorAsState(
        targetValue = palette.surfacePaper.copy(alpha = if (palette.isDark) 0.9f else 1f),
        animationSpec = tween(280),
        label = "CardBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = palette.lineSoft.copy(alpha = if (palette.isDark) 0.45f else 0.85f),
        animationSpec = tween(280),
        label = "CardBorder"
    )

    val revealBackground = colorLerp(cardBackground, Color.Black, revealProgress)
    val pressScale = when {
        revealProgress > 0f -> lerp(if (pressCharge > 0f) 0.97f else 1f, 1f, revealProgress)
        pressCharge > 0f -> 0.97f
        else -> 1f
    }

    val focusWeight = (1f - minOf(abs(relativePosition), 1f)).coerceIn(0f, 1f)
    val shadowRadius = lerp(layout.sideShadowRadius.value, layout.focusedShadowRadius.value, focusWeight)
    val shadowElevation = (shadowRadius + (shadowRadius * 1.15f - shadowRadius) * revealProgress).dp
    val shadowAlpha = if (palette.isDark) 0.35f else 0.12f + focusWeight * 0.06f

    LaunchedEffect(shouldAmbientBob) {
        bobAmplitude.animateTo(
            targetValue = if (shouldAmbientBob) 1f else 0f,
            animationSpec = if (shouldAmbientBob) {
                tween(durationMillis = AmbientBobRampInMs, easing = AmbientBobEaseIn)
            } else {
                tween(durationMillis = AmbientBobRampOutMs, easing = AmbientBobEaseOut)
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { timeNanos ->
                val t = timeNanos / 1_000_000_000.0
                val amplitude = bobAmplitude.value
                ambientOffsetX = (sin(t * 0.55) * 2.5f * amplitude).toFloat()
                ambientOffsetY = (sin(t * 0.72 + 0.6) * 4f * amplitude).toFloat()
            }
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(layout.cornerRadius),
                ambientColor = Color.Black.copy(alpha = shadowAlpha),
                spotColor = Color.Black.copy(alpha = shadowAlpha),
                clip = false
            )
            .graphicsLayer {
                translationX = ambientOffsetX
                translationY = ambientOffsetY
            }
            .scale(pressScale)
            .clip(RoundedCornerShape(layout.cornerRadius))
            .background(revealBackground)
            .border(1.dp, borderColor, RoundedCornerShape(layout.cornerRadius))
    ) {
        val imageSpanHeight = layout.heroHeight + layout.transitionBlendHeight
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 1f - revealProgress }
            ) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(imageSpanHeight)
                        .align(Alignment.TopCenter)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = heroFadeMaskBrush(layout),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    HeroImageStack(
                        feature = feature,
                        cardWidth = cardWidth,
                        totalHeight = imageSpanHeight,
                        layout = layout,
                        relativePosition = relativePosition,
                        pressCharge = pressCharge
                    )
                }

                CopyPaperBacking(
                    layout = layout,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                CopySection(
                    feature = feature,
                    layout = layout,
                    pressCharge = pressCharge,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = layout.heroHeight + layout.copySectionOffset)
                )

                if (pressCharge > 0f && revealProgress == 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = (1f + pressCharge * 0.6f).dp,
                                color = palette.textPrimary.copy(alpha = 0.08f + pressCharge * 0.14f),
                                shape = RoundedCornerShape(layout.cornerRadius)
                            )
                    )
                }
            }

            if (revealProgress > 0f) {
                HeroImage(
                    feature = feature,
                    cardWidth = cardWidth,
                    height = cardHeight,
                    relativePosition = relativePosition,
                    parallaxScale = lerp(1f, 0.22f, revealProgress),
                    modifier = Modifier.graphicsLayer { alpha = revealProgress }
                )
            }
        }
    }
}

@Composable
private fun HeroImageStack(
    feature: OnboardingFeature,
    cardWidth: Dp,
    totalHeight: Dp,
    layout: CardLayoutMetrics,
    relativePosition: Float,
    pressCharge: Float
) {
    val palette = OnboardingTheme.palette
    val blendHeight = layout.transitionBlendHeight * 1.6f
    val imageScale = 1f + pressCharge * 0.05f
    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(totalHeight)
            .scale(imageScale)
    ) {
        HeroImage(
            feature = feature,
            cardWidth = cardWidth,
            height = totalHeight,
            relativePosition = relativePosition,
            parallaxScale = 1f
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(blendHeight)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to palette.surfacePaper.copy(alpha = if (palette.isDark) 0.22f else 0.28f),
                            1f to palette.surfacePaper.copy(alpha = if (palette.isDark) 0.55f else 0.62f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun HeroImage(
    feature: OnboardingFeature,
    cardWidth: Dp,
    height: Dp,
    relativePosition: Float,
    parallaxScale: Float,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(feature.imageRes),
        contentDescription = feature.title,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                translationX = relativePosition * -14f * parallaxScale
                translationY = relativePosition * -5f * parallaxScale
            },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun CopyPaperBacking(
    layout: CardLayoutMetrics,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(layout.heroHeight - layout.transitionBlendHeight * 0.45f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.copyHeight + layout.transitionBlendHeight * 0.55f)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to palette.surfacePaper.copy(alpha = 0f),
                            0.18f to palette.surfacePaper.copy(alpha = if (palette.isDark) 0.38f else 0.48f),
                            0.42f to palette.surfacePaper.copy(alpha = if (palette.isDark) 0.78f else 0.86f),
                            0.72f to palette.surfacePaper,
                            1f to palette.surfacePaper
                        )
                    )
                )
        )
    }
}

@Composable
private fun CopySection(
    feature: OnboardingFeature,
    layout: CardLayoutMetrics,
    pressCharge: Float,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val copyAlpha = 1f - pressCharge * 0.42f
    val copyBlur = (pressCharge * 1.6f).dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(layout.copyHeight)
            .graphicsLayer { alpha = copyAlpha }
            .then(
                if (pressCharge > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(copyBlur)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = layout.horizontalPadding)
            .padding(top = layout.copyTopPadding, bottom = layout.copyBottomPadding)
    ) {
        Text(
            text = feature.title,
            color = palette.textPrimary,
            fontSize = layout.titleFontSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = layout.titleFontSize * 1.15f
        )
        Text(
            text = feature.subtitle,
            color = palette.textPrimary.copy(alpha = if (palette.isDark) 0.82f else 0.78f),
            fontSize = layout.subtitleFontSize,
            fontWeight = FontWeight.Medium,
            lineHeight = layout.subtitleFontSize * 1.2f,
            modifier = Modifier.padding(top = layout.textSpacing)
        )
        Text(
            text = feature.description,
            color = palette.textSecondary,
            fontSize = layout.bodyFontSize,
            lineHeight = layout.bodyFontSize * 1.35f,
            modifier = Modifier.padding(top = layout.textSpacing)
        )
    }
}

private fun heroFadeMaskBrush(layout: CardLayoutMetrics): Brush {
    val split = (layout.heroHeight / (layout.heroHeight + layout.transitionBlendHeight)).coerceIn(0f, 1f)
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White,
            split * 0.94f to Color.White,
            split to Color.White.copy(alpha = 0.82f),
            (split + 0.55f * (1f - split)).coerceAtMost(1f) to Color.White.copy(alpha = 0.18f),
            1f to Color.Transparent
        )
    )
}

internal fun cardStepOffset(relative: Float, cardWidth: Dp): Dp {
    return (cardWidth + CardInterCardGapDp.dp) * relative
}

private data class CardLayoutMetrics(
    val cardWidth: Dp,
    val cardHeight: Dp
) {
    val heroHeight: Dp = cardHeight * 0.60f
    val copyHeight: Dp = cardHeight * 0.40f
    val copySectionOffset: Dp = cardHeight * 0.035f
    val transitionBlendHeight: Dp = cardHeight * 0.10f
    val cornerRadius: Dp = cardWidth * 0.074f
    val titleFontSize = (cardWidth.value * 0.052f).coerceAtLeast(18f).sp
    val subtitleFontSize = (cardWidth.value * 0.041f).coerceAtLeast(14f).sp
    val bodyFontSize = (cardWidth.value * 0.038f).coerceAtLeast(13f).sp
    val horizontalPadding: Dp = cardWidth * 0.058f
    val textSpacing: Dp = cardHeight * 0.016f
    val copyTopPadding: Dp = copyHeight * 0.06f
    val copyBottomPadding: Dp = copyHeight * 0.11f
    val focusedShadowRadius: Dp = cardWidth * 0.062f
    val focusedShadowY: Dp = cardHeight * 0.024f
    val sideShadowRadius: Dp = cardWidth * 0.028f
    val sideShadowY: Dp = cardHeight * 0.010f
}

private data class CardDimensions(val width: Dp, val height: Dp)

@Composable
private fun cardDimensions(maxWidth: Dp, maxHeight: Dp): CardDimensions {
    val height = maxHeight * CardHeightRatio
    val width = minOf(maxWidth * CardWidthRatio, height / CardAspectRatio)
    return CardDimensions(width = width, height = height)
}

internal fun modularRelative(featureIndex: Int, scroll: Float, count: Int): Float {
    if (count <= 1) return featureIndex - scroll
    val base = featureIndex - scroll
    var best = base
    for (shift in listOf(-count, 0, count)) {
        val candidate = base + shift
        if (abs(candidate) < abs(best)) best = candidate
    }
    return best
}

internal fun wrappedIndex(index: Int, count: Int): Int {
    if (count <= 0) return 0
    return ((index % count) + count) % count
}

internal fun normalizeScrollIndex(scroll: Float, count: Int): Float {
    if (count <= 0) return scroll
    var normalized = scroll % count
    if (normalized < 0) normalized += count
    return normalized
}

private fun visibleFeatureIndices(scroll: Float, count: Int, isCarouselMoving: Boolean): List<Int> {
    val window = if (isCarouselMoving) 1.2f else 1.05f
    return (0 until count).filter { abs(modularRelative(it, scroll, count)) <= window }
}
