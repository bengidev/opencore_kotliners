package io.github.bengidev.opencore.onboarding.presenter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.application.OnboardingState
import io.github.bengidev.opencore.onboarding.domain.OnboardingPage
import io.github.bengidev.opencore.onboarding.domain.OnboardingPageType
import io.github.bengidev.opencore.onboarding.domain.OnboardingQueueItem
import io.github.bengidev.opencore.onboarding.presenter.components.Badge
import io.github.bengidev.opencore.onboarding.presenter.components.CardChrome
import io.github.bengidev.opencore.onboarding.presenter.components.DiagonalHatchPattern
import io.github.bengidev.opencore.onboarding.presenter.components.HighlightFooter
import io.github.bengidev.opencore.onboarding.presenter.components.IndexLabelChip
import io.github.bengidev.opencore.onboarding.presenter.components.PixelGridBackground
import io.github.bengidev.opencore.ui.components.ScaleToFitText
import io.github.bengidev.opencore.onboarding.presenter.components.TerminalHeader
import io.github.bengidev.opencore.onboarding.presenter.components.iconForPageType
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import kotlinx.coroutines.delay

/** Page scaffold — iOS OnboardingFeaturePageView. */
@Composable
internal fun FeaturePageView(
    page: OnboardingPage,
    state: OnboardingState,
    compactHeight: Boolean,
    availableHeight: Dp,
    onPromptChipTapped: (Int) -> Unit,
    onPairingToggleTapped: () -> Unit,
    onAddQueuedPromptTapped: () -> Unit,
    onReasoningLevelChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette

    var appeared by remember(page.id) { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feature_alpha"
    )

    val slideOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 12f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feature_slide"
    )

    LaunchedEffect(page.id) {
        appeared = false
        delay(70)
        appeared = true
    }

    if (page.type == OnboardingPageType.WorkspaceReady) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    translationY = slideOffset
                }
        ) {
            PageVisualFactory(
                page = page,
                state = state,
                appeared = appeared,
                onPromptChipTapped = onPromptChipTapped,
                onPairingToggleTapped = onPairingToggleTapped,
                onReasoningLevelChanged = onReasoningLevelChanged,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val visualHeight = remember(compactHeight, availableHeight) {
        val ratio = if (compactHeight) 0.39f else 0.43f
        val minH = if (compactHeight) 270.dp else 326.dp
        val maxH = 390.dp
        (availableHeight * ratio).coerceIn(minH, maxH)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = slideOffset
            },
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(text = page.eyebrow, icon = iconForPageType(page.type))
            Spacer(modifier = Modifier.weight(1f))
            IndexLabelChip(label = page.indexLabel)
        }

        ScaleToFitText(
            text = page.headline,
            style = OnboardingTheme.typography.displayLg,
            color = palette.textPrimary,
            maxLines = 2,
            minFontSize = 32.sp,
            modifier = Modifier.fillMaxWidth()
        )

        ScaleToFitText(
            text = page.body,
            style = OnboardingTheme.typography.bodyMd.copy(fontSize = 13.5.sp, lineHeight = 18.sp),
            color = palette.textSecondary,
            maxLines = 3,
            minFontSize = 11.sp,
            modifier = Modifier.fillMaxWidth()
        )

        CardChrome(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualHeight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PixelGridBackground(
                    modifier = Modifier.fillMaxSize(),
                    spacing = 15.dp,
                    color = palette.textTertiary.copy(alpha = if (palette.isDark) 0.06f else 0.04f)
                )
                DiagonalHatchPattern(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.lineSoft.copy(alpha = if (palette.isDark) 0.08f else 0.03f)
                )

                if (page.type == OnboardingPageType.PromptQueue) {
                    PromptQueueCardContent(
                        page = page,
                        state = state,
                        appeared = appeared,
                        onAddQueuedPromptTapped = onAddQueuedPromptTapped,
                        onPromptChipTapped = onPromptChipTapped,
                        onPairingToggleTapped = onPairingToggleTapped,
                        onReasoningLevelChanged = onReasoningLevelChanged
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TerminalHeader(page = page)
                        PageVisualFactory(
                            page = page,
                            state = state,
                            appeared = appeared,
                            onPromptChipTapped = onPromptChipTapped,
                            onPairingToggleTapped = onPairingToggleTapped,
                            onReasoningLevelChanged = onReasoningLevelChanged,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        if (page.highlights.isNotEmpty()) {
                            HighlightFooter(highlights = page.highlights)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptQueueCardContent(
    page: OnboardingPage,
    state: OnboardingState,
    appeared: Boolean,
    onAddQueuedPromptTapped: () -> Unit,
    onPromptChipTapped: (Int) -> Unit,
    onPairingToggleTapped: () -> Unit,
    onReasoningLevelChanged: (Double) -> Unit
) {
    val queueFull = state.demoState.queuedPromptCount >= OnboardingQueueItem.samples.size
    val queueScrollState = rememberScrollState()

    LaunchedEffect(state.demoState.queuedPromptCount) {
        delay(80)
        val start = queueScrollState.value
        val end = queueScrollState.maxValue
        if (end <= start) return@LaunchedEffect
        val steps = 12
        repeat(steps) { step ->
            val fraction = FastOutSlowInEasing.transform((step + 1) / steps.toFloat())
            queueScrollState.scrollTo(start + ((end - start) * fraction).toInt())
            delay(28)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TerminalHeader(
            page = page,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .verticalScroll(queueScrollState),
            contentAlignment = Alignment.Center
        ) {
            PageVisualFactory(
                page = page,
                state = state,
                appeared = appeared,
                onPromptChipTapped = onPromptChipTapped,
                onPairingToggleTapped = onPairingToggleTapped,
                onReasoningLevelChanged = onReasoningLevelChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AddFollowUpQueueButton(
            queueFull = queueFull,
            onClick = onAddQueuedPromptTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        )

        if (page.highlights.isNotEmpty()) {
            HighlightFooter(
                highlights = page.highlights,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun AddFollowUpQueueButton(
    queueFull: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.968f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 340f),
        label = "add_followup_scale"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.18f else 0.11f,
        animationSpec = tween(durationMillis = 180),
        label = "add_followup_bg"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.42f else 0.28f,
        animationSpec = tween(durationMillis = 180),
        label = "add_followup_border"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 280f),
        label = "add_followup_icon_rotation"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.accentPrimary.copy(alpha = backgroundAlpha))
            .border(
                width = 1.dp,
                color = palette.accentPrimary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = palette.accentPrimary,
            modifier = Modifier
                .height(14.dp)
                .graphicsLayer { rotationZ = iconRotation }
        )
        Text(
            text = if (queueFull) "RESET QUEUE" else "ADD FOLLOW-UP",
            style = OnboardingTheme.typography.monoSm,
            color = palette.accentPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "⌘↩",
            style = OnboardingTheme.typography.monoXs,
            color = palette.textTertiary
        )
    }
}
