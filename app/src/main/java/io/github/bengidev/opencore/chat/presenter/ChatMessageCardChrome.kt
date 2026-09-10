package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.theme.ChatTheme

private val CardShape = RoundedCornerShape(14.dp)

/** Shared raised-card chrome for categorized chat stream bubbles. */
@Composable
internal fun ChatMessageCardChrome(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = ChatTheme.palette

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = palette.reasoningCard,
        border = BorderStroke(0.5.dp, palette.reasoningBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            content = content,
        )
    }
}
