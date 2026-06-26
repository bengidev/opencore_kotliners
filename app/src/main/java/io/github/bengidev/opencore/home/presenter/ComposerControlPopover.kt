package io.github.bengidev.opencore.home.presenter

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
internal fun rememberComposerControlPopoverPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gapPx = with(density) { 8.dp.roundToPx() }
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
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

@Composable
internal fun ComposerControlPopoverHost(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val popupPositionProvider = rememberComposerControlPopoverPositionProvider()
    Box(modifier = modifier) {
        anchor()
        if (expanded) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun ComposerControlPopoverShell(
    title: String,
    badge: String?,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val badgeStyle = typography.contextUsage.copy(
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )

    Column(
        modifier = modifier
            .widthIn(min = 232.dp, max = 280.dp)
            .homeComposerGlass(cornerRadius = 18.dp, shadowOpacity = 0.14f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = typography.chipLabel.copy(fontWeight = FontWeight.Medium),
                color = palette.textPrimary,
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = badgeStyle,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.55f else 0.85f))
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
internal fun ComposerControlProgressBar(fraction: Float) {
    val palette = HomeTheme.palette
    val trackColor = palette.lineSoft.copy(alpha = if (palette.isDark) 0.45f else 0.55f)
    val fillColor = palette.textPrimary.copy(alpha = if (palette.isDark) 0.55f else 0.35f)
    val clamped = fraction.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
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
