package io.github.bengidev.opencore.onboarding.presenter.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeature
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

@Composable
internal fun OnboardingFeatureOrb(
    feature: OnboardingFeature?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    val palette = OnboardingTheme.palette
    val orbColors = feature?.orbColors(palette) ?: listOf(palette.textTertiary)
    val icon = feature?.iconVector() ?: Icons.Outlined.Star
    val iconColor = if (palette.isDark) palette.surfaceBase else palette.controlStrongText

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = orbColors + orbColors.first()
                )
            )
            .border(1.dp, palette.lineSoft.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (palette.isDark) Color.White else palette.controlStrongText)
                                .copy(alpha = if (palette.isDark) 0.5f else 0.32f),
                            Color.Transparent
                        )
                    )
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun OnboardingFeature.iconVector(): ImageVector = when (iconName) {
    "cpu" -> Icons.Outlined.Memory
    "layers" -> Icons.Outlined.Layers
    "branch" -> Icons.Outlined.AccountTree
    "lock" -> Icons.Outlined.Lock
    else -> Icons.Outlined.Star
}

internal fun OnboardingFeature.orbColors(palette: OpenCorePalette): List<Color> {
    val ramps: List<List<Color>> = if (palette.isDark) {
        listOf(
            listOf(palette.textTertiary, palette.textSecondary, palette.accentPrimary),
            listOf(palette.surfaceSubtle, palette.lineStrong, palette.accentDeep),
            listOf(palette.lineSoft, palette.textSecondary, palette.accentPrimary),
            listOf(palette.surfaceGalaxyTint, palette.lineStrong, palette.textPrimary.copy(alpha = 0.9f))
        )
    } else {
        listOf(
            listOf(palette.textTertiary, palette.textSecondary, palette.accentDeep),
            listOf(palette.lineStrong, palette.textSecondary, palette.accentPrimary),
            listOf(palette.surfaceSubtle, palette.lineSoft, palette.accentDeep),
            listOf(palette.textTertiary, palette.accentPrimary, palette.accentDeep)
        )
    }
    return ramps[orbStyleIndex % ramps.size]
}
