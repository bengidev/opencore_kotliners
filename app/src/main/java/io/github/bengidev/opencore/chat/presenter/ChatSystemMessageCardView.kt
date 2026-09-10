package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer

/** Inline system notice bubble with distinct card chrome. */
@Composable
internal fun ChatSystemMessageCardView(
    content: String,
    modifier: Modifier = Modifier,
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography

    ChatMessageCardChrome(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = palette.streamingDot,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "System",
                    style = typography.reasoningHeader,
                    color = palette.reasoningText,
                )
            }

            ChatRichContentColumn(
                markdown = content,
                profile = ChatMarkwonRenderer.Profile.Assistant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
