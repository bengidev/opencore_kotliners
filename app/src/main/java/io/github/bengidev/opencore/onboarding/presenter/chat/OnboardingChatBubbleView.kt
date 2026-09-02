package io.github.bengidev.opencore.onboarding.presenter.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.domain.OnboardingChatMessage
import io.github.bengidev.opencore.onboarding.domain.OnboardingChatRole
import io.github.bengidev.opencore.onboarding.presenter.components.ShimmerText
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.onboarding.thinkingorbs.ThinkingOrbView
import io.github.bengidev.opencore.onboarding.thinkingorbs.thinkingOrbStateForIndex

private val CornerRadius = 20.dp
private val OppositeSpacerMinWidth = 52.dp
private const val MaxBubbleWidthRatio = 0.8f
private val ThinkingOrbDisplaySize = 34.dp

@Composable
internal fun OnboardingChatBubbleView(
    message: OnboardingChatMessage,
    containerWidth: Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val maxBubbleWidth = containerWidth * MaxBubbleWidthRatio

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        when (message.role) {
            OnboardingChatRole.USER -> {
                Spacer(modifier = Modifier.width(OppositeSpacerMinWidth))
                UserBubble(
                    text = message.text,
                    maxWidth = maxBubbleWidth,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            OnboardingChatRole.THINKING,
            OnboardingChatRole.ASSISTANT -> {
                LeftAlignedBubble(
                    message = message,
                    maxWidth = maxBubbleWidth,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(OppositeSpacerMinWidth))
            }
        }
    }
}

@Composable
private fun UserBubble(
    text: String,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            color = palette.controlStrongText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .width(maxWidth)
                .clip(RoundedCornerShape(CornerRadius))
                .background(palette.controlStrong)
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .semantics { contentDescription = text }
        )
    }
}

@Composable
private fun LeftAlignedBubble(
    message: OnboardingChatMessage,
    maxWidth: Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val assistantMessage = remember(message.id, message.feature, message.preparedAssistant) {
        message.preparedAssistant ?: message.morphToAssistant()
    }
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.roundToPx() }
    val shellCache = remember(message.id, assistantMessage.feature?.id, maxWidthPx) {
        AssistantShellCache()
    }

    SubcomposeLayout(modifier = modifier.width(maxWidth)) { constraints ->
        val widthConstraint = constraints.copy(maxWidth = maxWidthPx)
        val shellWidth: Int
        val shellHeight: Int
        if (shellCache.isValid) {
            shellWidth = shellCache.width
            shellHeight = shellCache.height
        } else {
            val assistantPlaceable = subcompose("assistant-measure") {
                AssistantBubble(message = assistantMessage, maxWidth = maxWidth)
            }.first().measure(widthConstraint)
            shellWidth = assistantPlaceable.width
            shellHeight = assistantPlaceable.height
            shellCache.set(shellWidth, shellHeight)
        }
        val shellHeightDp = with(density) { shellHeight.toDp() }

        val contentPlaceable = subcompose("bubble-content") {
            Box(
                modifier = Modifier
                    .width(maxWidth)
                    .height(shellHeightDp),
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedContent(
                    targetState = message.role,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            (fadeIn(spring(dampingRatio = 0.82f, stiffness = 170f)) +
                                scaleIn(
                                    initialScale = 0.98f,
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 170f)
                                )) togetherWith
                                fadeOut(animationSpec = spring(dampingRatio = 0.82f, stiffness = 170f))
                        }
                    },
                    label = "OnboardingChatBubbleMorph"
                ) { role ->
                    when (role) {
                        OnboardingChatRole.THINKING -> ThinkingBubble(
                            message = message,
                            maxWidth = maxWidth,
                            reduceMotion = reduceMotion
                        )
                        OnboardingChatRole.ASSISTANT -> AssistantBubble(
                            message = assistantMessage,
                            maxWidth = maxWidth
                        )
                        OnboardingChatRole.USER -> Unit
                    }
                }
            }
        }.first().measure(
            widthConstraint.copy(
                minWidth = shellWidth,
                maxWidth = shellWidth,
                minHeight = shellHeight,
                maxHeight = shellHeight
            )
        )

        layout(shellWidth, shellHeight) {
            contentPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun ThinkingBubble(
    message: OnboardingChatMessage,
    maxWidth: Dp,
    reduceMotion: Boolean
) {
    val palette = OnboardingTheme.palette
    Row(
        modifier = Modifier
            .width(maxWidth)
            .clip(RoundedCornerShape(CornerRadius))
            .background(palette.surfacePaper.copy(alpha = if (palette.isDark) 0.92f else 1f))
            .border(1.dp, palette.lineSoft, RoundedCornerShape(CornerRadius))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Thinking" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThinkingOrbView(
            state = message.feature?.let { thinkingOrbStateForIndex(it.orbStyleIndex) }
                ?: thinkingOrbStateForIndex(0),
            isDark = palette.isDark,
            reduceMotion = reduceMotion,
            displaySize = ThinkingOrbDisplaySize
        )
        ShimmerText(
            text = "Thinking…",
            baseColor = palette.textSecondary,
            isActive = true,
            reduceMotion = reduceMotion,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AssistantBubble(
    message: OnboardingChatMessage,
    maxWidth: Dp
) {
    val palette = OnboardingTheme.palette
    val feature = message.feature

    Row(
        modifier = Modifier
            .width(maxWidth)
            .clip(RoundedCornerShape(CornerRadius))
            .background(palette.surfacePaper.copy(alpha = if (palette.isDark) 0.92f else 1f))
            .border(1.dp, palette.lineSoft, RoundedCornerShape(CornerRadius))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics {
                contentDescription = feature?.feedAccessibilityLabel ?: message.text
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OnboardingFeatureOrb(feature = feature)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = feature?.title.orEmpty(),
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = feature?.subtitle.orEmpty(),
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            val description = feature?.description.orEmpty()
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private class AssistantShellCache {
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var isValid: Boolean = false
        private set

    fun set(width: Int, height: Int) {
        this.width = width
        this.height = height
        isValid = true
    }
}
