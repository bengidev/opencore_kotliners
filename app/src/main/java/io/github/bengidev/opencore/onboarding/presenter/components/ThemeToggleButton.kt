package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.onboarding.theme.AppTheme
import io.github.bengidev.opencore.onboarding.theme.LocalAppTheme
import kotlinx.coroutines.delay

private val TrackWidth = 32.dp
private val TrackHeight = 28.dp
private val ThumbWidth = 11.dp
private val ThumbInset = 3.dp
private val ThumbHeight = 22.dp
private val ThumbCenterOffset = (TrackWidth - ThumbWidth) / 2

/** Theme toggle track — matches iOS ThemeToggleButton (32×28, sliding accent thumb). */
@Composable
internal fun ThemeToggleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val appTheme = LocalAppTheme.current
    val isSystemMode = appTheme == AppTheme.System
    val isDark = palette.isDark

    var tapped by remember { mutableStateOf(false) }

    val thumbOffset by animateDpAsState(
        targetValue = when {
            isSystemMode -> ThumbCenterOffset
            isDark -> TrackWidth - ThumbWidth - ThumbInset
            else -> ThumbInset
        },
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ThemeToggleThumbOffset"
    )

    val trackScale by animateFloatAsState(
        targetValue = if (tapped) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ThemeToggleTrackScale"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (tapped) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ThemeToggleThumbScale"
    )

    val thumbRotation by animateFloatAsState(
        targetValue = if (tapped) -8f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ThemeToggleThumbRotation"
    )

    LaunchedEffect(tapped) {
        if (tapped) {
            delay(120)
            tapped = false
            onClick()
        }
    }

    Box(
        modifier = modifier
            .width(TrackWidth)
            .height(TrackHeight)
            .scale(trackScale)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.surfaceSubtle.copy(alpha = 0.5f))
            .border(
                width = if (isSystemMode) 0.8.dp else 0.5.dp,
                color = if (isSystemMode) palette.lineStrong else palette.lineSoft,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!tapped) tapped = true
            }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = ThumbInset)
                .width(ThumbWidth)
                .height(ThumbHeight)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                    rotationZ = thumbRotation
                }
                .shadow(
                    elevation = if (isSystemMode) 2.dp else 4.dp,
                    shape = RoundedCornerShape(3.dp),
                    ambientColor = palette.accentPrimary.copy(alpha = if (isSystemMode) 0.30f else 0.50f),
                    spotColor = palette.accentPrimary.copy(alpha = if (isSystemMode) 0.30f else 0.50f)
                )
                .clip(RoundedCornerShape(3.dp))
                .background(palette.accentPrimary)
                .border(
                    width = 0.5.dp,
                    color = palette.textPrimary.copy(alpha = if (isSystemMode) 0.08f else 0.18f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}
