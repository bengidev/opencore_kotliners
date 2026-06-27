package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.home.presenter.components.homeComposerGlass
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun ContextWindowPopover(
    usage: ContextWindowUsage,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val metricStyle = typography.contextUsage.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
    )
    val footerStyle = typography.contextUsage.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
    )
    val badgeStyle = typography.contextUsage.copy(
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
    )

    Column(
        modifier = modifier
            .width(260.dp)
            .homeComposerGlass(cornerRadius = 28.dp, shadowOpacity = 0.14f)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Context window",
                style = typography.chipLabel.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = palette.textSecondary,
            )
            if (usage.hasKnownLimit) {
                Text(
                    text = "${usage.percentRemaining}% left",
                    style = badgeStyle,
                    color = palette.accentPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.accentPrimary.copy(alpha = if (palette.isDark) 0.18f else 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        if (usage.hasKnownLimit) {
            ContextUsageProgressBar(fraction = usage.fractionUsed.toFloat())
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ContextUsageMetricRow(
                label = "Free",
                value = if (usage.hasKnownLimit) usage.tokensRemainingFormatted else "—",
                valueStyle = metricStyle,
            )
            ContextUsageMetricRow(
                label = "Used",
                value = usage.tokensUsedFormatted,
                valueStyle = metricStyle,
            )
            ContextUsageMetricRow(
                label = "Total",
                value = if (usage.hasKnownLimit) usage.tokenLimitFormatted else "—",
                valueStyle = metricStyle,
            )
        }

        if (usage.hasKnownLimit) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${usage.percentUsed}% used",
                    style = footerStyle,
                    color = palette.textPrimary,
                )
                Text(
                    text = "${usage.percentRemaining}% left",
                    style = footerStyle,
                    color = palette.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun ContextUsageMetricRow(
    label: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
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
private fun ContextUsageProgressBar(fraction: Float) {
    val palette = HomeTheme.palette
    val trackColor = palette.lineSoft.copy(alpha = if (palette.isDark) 0.35f else 0.55f)
    val fillColor = palette.accentPrimary.copy(alpha = if (palette.isDark) 0.92f else 0.82f)
    val clamped = fraction.coerceIn(0f, 1f)

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fillColor),
            )
        }
    }
}
