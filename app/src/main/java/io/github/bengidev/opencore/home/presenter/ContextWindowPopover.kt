package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.models.ContextWindowUsage
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

    ComposerControlPopoverShell(
        title = "Context window",
        badge = if (usage.hasKnownLimit) "${usage.percentRemaining}% left" else null,
        modifier = modifier.width(260.dp),
        cornerRadius = 28.dp,
        prominentBadge = true,
        footer = if (usage.hasKnownLimit) {
            {
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
        } else {
            null
        },
    ) {
        if (usage.hasKnownLimit) {
            ComposerControlProgressBar(
                fraction = usage.fractionUsed.toFloat(),
                height = 10.dp,
                cornerRadius = 10.dp,
                trackColorOverride = palette.lineSoft.copy(alpha = if (palette.isDark) 0.35f else 0.55f),
                fillColorOverride = palette.accentPrimary.copy(alpha = if (palette.isDark) 0.92f else 0.82f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ComposerControlPopoverMetricRow(
                label = "Free:",
                value = if (usage.hasKnownLimit) usage.tokensRemainingFormatted else "—",
                valueStyle = metricStyle,
            )
            ComposerControlPopoverMetricRow(
                label = "Used:",
                value = usage.tokensUsedFormatted,
                valueStyle = metricStyle,
            )
            ComposerControlPopoverMetricRow(
                label = "Total:",
                value = if (usage.hasKnownLimit) usage.tokenLimitFormatted else "—",
                valueStyle = metricStyle,
            )
        }
    }
}
