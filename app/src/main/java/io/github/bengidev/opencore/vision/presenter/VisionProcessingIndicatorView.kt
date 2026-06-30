package io.github.bengidev.opencore.vision.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun VisionProcessingIndicatorView(
    statusMessage: String?,
    modifier: Modifier = Modifier,
) {
    val message = statusMessage ?: return
    val palette = HomeTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.5f else 0.8f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = palette.accentPrimary,
        )
        Text(
            text = message,
            style = HomeTheme.typography.chipLabel,
            color = palette.textSecondary,
        )
    }
}
