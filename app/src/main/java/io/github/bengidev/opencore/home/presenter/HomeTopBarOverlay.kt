package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme

internal val HomeTopBarClearance = 52.dp

@Composable
internal fun HomeTopBarOverlay(
    onSidebarTapped: () -> Unit,
    onNewConversationTapped: () -> Unit,
    onDismissKeyboard: () -> Unit,
    threadTitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures { onDismissKeyboard() }
            }
    ) {
        HomeTopBarButton(
            onClick = onSidebarTapped,
            icon = Icons.Default.Menu,
            contentDescription = "Show sidebar",
            modifier = Modifier.align(Alignment.CenterStart)
        )
        HomeTopBarButton(
            onClick = onNewConversationTapped,
            icon = Icons.Default.Add,
            contentDescription = "New conversation",
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        if (!threadTitle.isNullOrBlank()) {
            Text(
                text = threadTitle,
                style = HomeTheme.typography.welcomeCaption,
                color = HomeTheme.palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )
        }
    }
}

@Composable
private fun HomeTopBarButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
