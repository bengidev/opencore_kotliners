package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun ContextWindowPopover(
    usage: ContextWindowUsage,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val valueStyle = typography.contextUsage.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )
    val footerStyle = typography.contextUsage.copy(
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal,
    )

    ComposerControlPopoverShell(
        title = "Context window",
        badge = if (usage.hasKnownLimit) "${usage.percentRemaining}% left" else null,
        modifier = modifier,
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
                        color = palette.textTertiary,
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
            ComposerControlProgressBar(fraction = usage.fractionUsed.toFloat())
        }
        ComposerControlPopoverMetricRow(
            label = "Free",
            value = if (usage.hasKnownLimit) usage.tokensRemainingFormatted else "—",
            valueStyle = valueStyle,
        )
        ComposerControlPopoverMetricRow(
            label = "Used",
            value = usage.tokensUsedFormatted,
            valueStyle = valueStyle,
        )
        ComposerControlPopoverMetricRow(
            label = "Total",
            value = if (usage.hasKnownLimit) usage.tokenLimitFormatted else "—",
            valueStyle = valueStyle,
        )
    }
}
