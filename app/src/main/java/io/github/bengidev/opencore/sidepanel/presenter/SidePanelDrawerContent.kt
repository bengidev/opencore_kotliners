package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.sidepanel.application.SidePanelState
import io.github.bengidev.opencore.sidepanel.domain.SessionItem
import io.github.bengidev.opencore.sidepanel.theme.SidePanelTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SidePanelDrawerContent(
    state: SidePanelState,
    onSessionTapped: (String) -> Unit,
    onSettingsTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        SidePanelDrawerHeader(onSettingsTapped = onSettingsTapped)
        HorizontalDivider(color = SidePanelTheme.palette.lineSoft)
        LazyColumn {
            items(state.sessions, key = { it.id }) { session ->
                SidePanelSessionRow(
                    session = session,
                    onClick = { onSessionTapped(session.id) }
                )
            }
        }
    }
}

@Composable
private fun SidePanelDrawerHeader(onSettingsTapped: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SESSIONS",
            style = SidePanelTheme.typography.monoLabel,
            color = SidePanelTheme.palette.textSecondary
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onSettingsTapped,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Settings" }
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = SidePanelTheme.palette.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SidePanelSessionRow(session: SessionItem, onClick: () -> Unit) {
    val bg = if (session.isActive) SidePanelTheme.palette.surfaceSubtle else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = session.title,
            style = SidePanelTheme.typography.sessionTitle,
            color = SidePanelTheme.palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = session.preview,
            style = SidePanelTheme.typography.sessionPreview,
            color = SidePanelTheme.palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
