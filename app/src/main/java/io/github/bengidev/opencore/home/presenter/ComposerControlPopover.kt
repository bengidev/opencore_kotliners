package io.github.bengidev.opencore.home.presenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.home.presenter.components.homeComposerGlass
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort

@Composable
internal fun rememberComposerControlPopoverPositionProvider(
    anchorAlignment: PopoverAnchorAlignment = PopoverAnchorAlignment.Center,
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density, anchorAlignment) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gapPx = with(density) { 8.dp.roundToPx() }
                val x = when (anchorAlignment) {
                    PopoverAnchorAlignment.Center ->
                        anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                    PopoverAnchorAlignment.Trailing ->
                        anchorBounds.right - popupContentSize.width
                }
                val aboveY = anchorBounds.top - popupContentSize.height - gapPx
                val y = if (aboveY >= 0) {
                    aboveY
                } else {
                    anchorBounds.bottom + gapPx
                }
                return IntOffset(
                    x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                )
            }
        }
    }
}

internal enum class PopoverAnchorAlignment {
    Center,
    Trailing,
}

@Composable
internal fun ComposerControlPopoverHost(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    anchorAlignment: PopoverAnchorAlignment = PopoverAnchorAlignment.Center,
    animateContent: Boolean = false,
    reduceMotion: Boolean = false,
    anchor: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val popupPositionProvider = rememberComposerControlPopoverPositionProvider(anchorAlignment)
    Box(modifier = modifier) {
        anchor()
        if (expanded) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                if (animateContent) {
                    ComposerControlPopoverEnterAnimation(reduceMotion = reduceMotion, content = content)
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ComposerControlPopoverEnterAnimation(
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val spec = HomeContextUsagePopoverMotion.presentationAnimationSpec(reduceMotion)
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) {
            fadeIn(spec)
        } else {
            fadeIn(spec) + scaleIn(
                animationSpec = spec,
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(1f, 1f),
            )
        },
    ) {
        content()
    }
}

@Composable
internal fun ComposerControlPopoverShell(
    title: String,
    badge: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    prominentBadge: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val badgeStyle = typography.contextUsage.copy(
        fontSize = if (prominentBadge) 12.sp else 9.sp,
        lineHeight = if (prominentBadge) 14.sp else 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = if (prominentBadge) FontFamily.Monospace else FontFamily.Default,
    )

    Column(
        modifier = modifier
            .widthIn(min = 232.dp, max = 280.dp)
            .homeComposerGlass(cornerRadius = cornerRadius, shadowOpacity = 0.14f)
            .padding(horizontal = if (cornerRadius >= 24.dp) 12.dp else 16.dp, vertical = if (cornerRadius >= 24.dp) 11.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (cornerRadius >= 24.dp) 12.dp else 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = typography.chipLabel.copy(
                    fontWeight = if (prominentBadge) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = if (prominentBadge) 11.sp else typography.chipLabel.fontSize,
                ),
                color = if (prominentBadge) palette.textSecondary else palette.textPrimary,
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = badgeStyle,
                    color = if (prominentBadge) palette.accentPrimary else palette.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            when {
                                prominentBadge -> palette.accentPrimary.copy(alpha = if (palette.isDark) 0.18f else 0.12f)
                                else -> palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.55f else 0.85f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        content()
        footer?.invoke()
    }
}

@Composable
internal fun ComposerControlPopoverMetricRow(
    label: String,
    value: String,
    valueStyle: TextStyle,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.chipLabel,
            color = palette.textSecondary,
        )
        Text(
            text = value,
            style = valueStyle,
            color = palette.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ComposerControlPopoverOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.chipLabel,
            color = if (selected) palette.textPrimary else palette.textSecondary,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = palette.accentPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun ComposerControlProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    cornerRadius: Dp = 999.dp,
    trackColorOverride: Color? = null,
    fillColorOverride: Color? = null,
) {
    val palette = HomeTheme.palette
    val trackColor = trackColorOverride
        ?: palette.lineSoft.copy(alpha = if (palette.isDark) 0.45f else 0.55f)
    val fillColor = fillColorOverride
        ?: palette.textPrimary.copy(alpha = if (palette.isDark) 0.55f else 0.35f)
    val clamped = fraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(fillColor),
            )
        }
    }
}

@Composable
internal fun ReasoningControlPopover(
    selectedEffort: ModelReasoningEffort,
    availableEfforts: List<ModelReasoningEffort>,
    onEffortSelected: (ModelReasoningEffort) -> Unit,
) {
    ComposerControlPopoverShell(
        title = "Reasoning",
        badge = selectedEffort.title,
    ) {
        availableEfforts.forEach { effort ->
            ComposerControlPopoverOptionRow(
                label = effort.title,
                selected = effort == selectedEffort,
                onClick = { onEffortSelected(effort) },
            )
        }
    }
}

@Composable
internal fun SpeedControlPopover(
    speedMode: HomeComposerSpeedMode,
    onSpeedModeSelected: (HomeComposerSpeedMode) -> Unit,
) {
    ComposerControlPopoverShell(
        title = "Speed",
        badge = speedMode.title,
    ) {
        HomeComposerSpeedMode.entries.forEach { mode ->
            ComposerControlPopoverOptionRow(
                label = mode.title,
                selected = mode == speedMode,
                onClick = { onSpeedModeSelected(mode) },
            )
        }
    }
}
