package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun HomeTopBar(
    onSidebarTapped: () -> Unit,
    onNewConversationTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSidebarTapped,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Show sidebar" }
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onNewConversationTapped,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "New conversation" }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
