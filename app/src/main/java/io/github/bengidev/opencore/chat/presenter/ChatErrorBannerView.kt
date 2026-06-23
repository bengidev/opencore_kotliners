package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.home.theme.HomeTheme

/** Turn-level failure banner — mirrors iOS `ChatErrorBannerView`. */
@Composable
internal fun ChatErrorBannerView(
    streamingStatus: ChatStreamingStatus,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (streamingStatus != ChatStreamingStatus.Failed || errorMessage.isNullOrBlank()) return

    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat-error-banner"),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = palette.danger,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Couldn't get a response",
                style = typography.chipLabel.copy(fontSize = typography.chipLabel.fontSize * 1.05f),
                color = palette.textPrimary
            )
            Text(
                text = errorMessage,
                style = typography.chipLabel,
                color = palette.textSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.controlStrong,
                    contentColor = palette.controlStrongText
                ),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text(text = "Retry", style = typography.chipLabel)
            }
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss", style = typography.chipLabel, color = palette.textSecondary)
            }
        }
    }
}
