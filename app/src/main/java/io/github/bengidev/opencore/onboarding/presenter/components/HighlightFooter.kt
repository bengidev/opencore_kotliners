package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeatureHighlight
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Two-tile highlight footer inside demo card — iOS highlightFooter. */
@Composable
internal fun HighlightFooter(
    highlights: List<OnboardingFeatureHighlight>,
    modifier: Modifier = Modifier,
    appeared: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        highlights.take(2).forEachIndexed { index, highlight ->
            HighlightTile(
                highlight = highlight,
                isFirst = index == 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HighlightTile(
    highlight: OnboardingFeatureHighlight,
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnboardingTheme.palette
    val iconBg = if (isFirst) palette.accentSoft else palette.surfaceSubtle.copy(alpha = 0.4f)
    val iconTint = if (isFirst) palette.accentPrimary else palette.textSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfacePaper)
            .border(1.dp, palette.lineSoft, RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForHighlight(highlight.title),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight.title.uppercase(),
                style = OnboardingTheme.typography.monoXs,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ScaleToFitText(
                text = highlight.detail,
                style = OnboardingTheme.typography.monoXs.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                ),
                color = palette.textTertiary,
                maxLines = 1,
                minFontSize = 8.sp
            )
        }
    }
}

private fun iconForHighlight(title: String): ImageVector {
    return when (title.lowercase()) {
        "local memory" -> Icons.Filled.Menu
        "secure session" -> Icons.Filled.Shield
        "design canvas" -> Icons.Filled.Layers
        "ai assistance" -> Icons.Filled.AutoAwesome
        "state engine" -> Icons.Filled.Menu
        "run steering" -> Icons.AutoMirrored.Filled.ArrowForward
        "model controls" -> Icons.Filled.Settings
        "human steering" -> Icons.Filled.Person
        else -> Icons.Filled.PlayArrow
    }
}
