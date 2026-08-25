package io.github.bengidev.opencore.onboarding.presenter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import io.github.bengidev.opencore.onboarding.presenter.carousel.FeatureCardCarousel
import io.github.bengidev.opencore.onboarding.presenter.components.SwipeToStartView
import io.github.bengidev.opencore.onboarding.presenter.cube.OnboardingCubeView
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.ui.components.ThemeToggleButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HeroLargeSize = 220.dp
private val HeroSmallSize = 36.dp
private val HeaderCubeTextGap = 14.dp
private val HeaderTopPadding = 18.dp
private val HeaderHorizontalPadding = 24.dp
private val FooterBottomPadding = 2.dp
private const val SwipeSectionDelayMs = 620L
private const val HeroShowoffDelayMs = 1750L
private const val HeroTransitionDurationMs = 1020
// Matches iOS `.smooth(duration: 1.02, extraBounce: 0)`.
private val HeroTransitionEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
internal fun OnboardingSinglePageScreen(
    darkTheme: Boolean,
    reduceMotion: Boolean,
    onThemeToggle: () -> Unit,
    onComplete: suspend () -> Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val backgroundColor by animateColorAsState(
        targetValue = palette.surfaceBase,
        animationSpec = tween(280),
        label = "OnboardingSurface"
    )
    val density = LocalDensity.current
    val view = LocalView.current

    var cubeAppeared by remember { mutableStateOf(false) }
    val heroTransitionAnim = remember { Animatable(0f) }
    var isTransformed by remember { mutableStateOf(false) }
    var showCarousel by remember { mutableStateOf(false) }
    var carouselRevealed by remember { mutableStateOf(false) }
    var swipeCompleted by remember { mutableStateOf(false) }

    val heroTransitionAnimated = heroTransitionAnim.value

    val headerAlpha by animateFloatAsState(
        targetValue = if (isTransformed) 1f else 0f,
        animationSpec = if (reduceMotion) tween(250) else tween(500, delayMillis = 120),
        label = "HeaderAlpha"
    )

    val swipeAlpha by animateFloatAsState(
        targetValue = if (isTransformed) 1f else 0f,
        animationSpec = if (reduceMotion) {
            tween(200)
        } else {
            tween(
                durationMillis = 600,
                delayMillis = SwipeSectionDelayMs.toInt(),
                easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
            )
        },
        label = "SwipeAlpha"
    )

    val carouselAlpha by animateFloatAsState(
        targetValue = if (carouselRevealed) 1f else 0f,
        animationSpec = if (reduceMotion) {
            tween(200)
        } else {
            spring(dampingRatio = 0.78f, stiffness = 320f)
        },
        label = "CarouselAlpha"
    )

    LaunchedEffect(reduceMotion) {
        cubeAppeared = true
        if (!reduceMotion) delay(HeroShowoffDelayMs)
        isTransformed = true
        showCarousel = true

        launch {
            if (reduceMotion) {
                heroTransitionAnim.snapTo(1f)
            } else {
                heroTransitionAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = HeroTransitionDurationMs,
                        easing = HeroTransitionEasing
                    )
                )
            }
        }

        if (!reduceMotion) delay(SwipeSectionDelayMs)
        carouselRevealed = true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val largeSizePx = with(density) { HeroLargeSize.toPx() }
        val smallSizePx = with(density) { HeroSmallSize.toPx() }
        val headerTopPx = with(density) { HeaderTopPadding.toPx() }
        val headerHorizontalPx = with(density) { HeaderHorizontalPadding.toPx() }

        val heroLayout = HeroCubeLayoutMath.layoutForTransition(
            transition = heroTransitionAnimated,
            width = widthPx,
            height = heightPx,
            largeSize = largeSizePx,
            smallSize = smallSizePx,
            headerTopPadding = headerTopPx,
            headerHorizontalPadding = headerHorizontalPx
        )

        val rotationProgress = HeroCubeLayoutMath.heroRotationProgress(heroTransitionAnimated)
        val morphPaused = showCarousel && carouselRevealed

        Column(modifier = Modifier.fillMaxSize()) {
            if (isTransformed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HeaderTopPadding, bottom = 2.dp)
                        .padding(horizontal = HeaderHorizontalPadding)
                        .graphicsLayer {
                            alpha = headerAlpha
                            translationX = if (isTransformed) 0f else -14f
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(HeroSmallSize))
                    Text(
                        text = "OPENCORE",
                        modifier = Modifier.padding(start = HeaderCubeTextGap),
                        color = palette.textPrimary,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.04.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            if (isTransformed && showCarousel) {
                FeatureCardCarousel(
                    isActive = true,
                    reduceMotion = reduceMotion,
                    contentRevealed = carouselRevealed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .graphicsLayer { alpha = carouselAlpha }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isTransformed) {
                SwipeToStartView(
                    isUnlocked = swipeCompleted,
                    reduceMotion = reduceMotion,
                    onComplete = {
                        val succeeded = onComplete()
                        if (succeeded) {
                            swipeCompleted = true
                            ViewCompat.performHapticFeedback(
                                view,
                                HapticFeedbackConstantsCompat.CONFIRM
                            )
                        }
                        succeeded
                    },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = FooterBottomPadding)
                        .graphicsLayer {
                            alpha = swipeAlpha
                            translationY = if (isTransformed) 0f else 28f
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = HeaderTopPadding, end = HeaderHorizontalPadding),
            contentAlignment = Alignment.TopEnd
        ) {
            ThemeToggleButton(
                palette = palette,
                isDark = darkTheme,
                onClick = onThemeToggle
            )
        }

        OnboardingCubeView(
            appeared = cubeAppeared,
            inkColor = palette.textPrimary,
            morphPaused = morphPaused,
            rotationProgress = rotationProgress,
            reduceMotion = reduceMotion,
            modifier = Modifier.heroCubeOverlay(heroLayout)
        )
    }
}
