package io.github.bengidev.opencore.onboarding.presenter.visuals

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeatureHighlight
import io.github.bengidev.opencore.onboarding.domain.OnboardingPage
import io.github.bengidev.opencore.onboarding.presenter.components.ScaleToFitText
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay

/** iOS OnboardingWorkspaceReadyVisualView — centered hero, capsule tags. */
@Composable
internal fun WorkspaceReadyVisualView(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
    appeared: Boolean = true
) {
    val palette = OnboardingTheme.palette
    val typography = OnboardingTheme.typography

    var chipVisible by remember(page.id) { mutableStateOf(false) }
    var headlineVisible by remember(page.id) { mutableStateOf(false) }
    var bodyVisible by remember(page.id) { mutableStateOf(false) }
    var tagsVisible by remember(page.id) { mutableStateOf(false) }

    LaunchedEffect(page.id, appeared) {
        if (!appeared) {
            chipVisible = false
            headlineVisible = false
            bodyVisible = false
            tagsVisible = false
            return@LaunchedEffect
        }
        chipVisible = false
        headlineVisible = false
        bodyVisible = false
        tagsVisible = false
        delay(50)
        chipVisible = true
        delay(70)
        headlineVisible = true
        delay(80)
        bodyVisible = true
        delay(80)
        tagsVisible = true
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AnimatedFadeSlide(visible = chipVisible, slideOffset = 10f) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.surfaceSubtle.copy(alpha = 0.5f))
                        .border(1.dp, palette.lineSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary)
                    )
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "WORKSPACE READY",
                        style = typography.monoSm.copy(fontSize = 13.sp),
                        color = palette.textTertiary
                    )
                }
            }

            AnimatedFadeSlide(visible = headlineVisible, slideOffset = 14f) {
                ScaleToFitText(
                    text = page.headline,
                    style = typography.displayXl,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minFontSize = 40.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedFadeSlide(visible = bodyVisible, slideOffset = 10f) {
                ScaleToFitText(
                    text = page.body,
                    style = typography.bodyLg.copy(lineHeight = 28.sp),
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    minFontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            AnimatedFadeSlide(visible = tagsVisible, slideOffset = 8f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    page.highlights.forEach { highlight ->
                        FeatureTag(highlight = highlight)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeatureTag(highlight: OnboardingFeatureHighlight) {
    val palette = OnboardingTheme.palette

    ScaleToFitText(
        text = highlight.title.uppercase(),
        style = OnboardingTheme.typography.monoSm.copy(fontSize = 12.sp),
        color = palette.textSecondary,
        maxLines = 1,
        minFontSize = 9.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surfaceSubtle.copy(alpha = 0.3f))
            .border(1.dp, palette.lineSoft, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

@Composable
private fun AnimatedFadeSlide(
    visible: Boolean,
    slideOffset: Float,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
        label = "fade_slide_alpha"
    )
    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else slideOffset,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
        label = "fade_slide_offset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offset
        }
    ) {
        content()
    }
}
