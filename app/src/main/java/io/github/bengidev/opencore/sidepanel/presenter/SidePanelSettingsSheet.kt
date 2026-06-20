package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.sidepanel.application.SidePanelState
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider
import io.github.bengidev.opencore.sidepanel.theme.SidePanelTheme

@Composable
internal fun SidePanelSettingsSheet(
    state: SidePanelState,
    onProviderSelected: (SessionProvider) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onSaveTapped: () -> Unit,
    onRemoveTapped: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SidePanelTheme.palette.surfaceRaised,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "SETTINGS",
                style = SidePanelTheme.typography.settingsTitle,
                color = SidePanelTheme.palette.textPrimary
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Provider",
                style = SidePanelTheme.typography.settingsLabel,
                color = SidePanelTheme.palette.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            SidePanelProviderPicker(
                selectedProvider = state.selectedProvider,
                onSelect = onProviderSelected
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "API key",
                style = SidePanelTheme.typography.settingsLabel,
                color = SidePanelTheme.palette.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            SidePanelApiKeyField(
                value = state.apiKeyInput,
                onValueChange = onApiKeyChanged
            )
            Spacer(Modifier.height(16.dp))
            if (state.isKeyStored) {
                Text(
                    "Key stored",
                    style = SidePanelTheme.typography.settingsBody,
                    color = SidePanelTheme.palette.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SidePanelActionButton(
                        text = "Update key",
                        onClick = onSaveTapped,
                        primary = true,
                        modifier = Modifier.weight(1f)
                    )
                    SidePanelActionButton(
                        text = "Remove stored key",
                        onClick = onRemoveTapped,
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                SidePanelActionButton(
                    text = "Save key",
                    onClick = onSaveTapped,
                    primary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SidePanelProviderPicker(
    selectedProvider: SessionProvider,
    onSelect: (SessionProvider) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SessionProvider.entries.forEach { provider ->
            val selected = provider == selectedProvider
            val bg = if (selected) SidePanelTheme.palette.controlStrong
            else SidePanelTheme.palette.surfaceSubtle.copy(alpha = 0.5f)
            val fg = if (selected) SidePanelTheme.palette.controlStrongText
            else SidePanelTheme.palette.textSecondary
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable { onSelect(provider) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    provider.displayName.uppercase(),
                    style = SidePanelTheme.typography.chipLabel,
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SidePanelApiKeyField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = SidePanelTheme.typography.settingsBody.copy(
            color = SidePanelTheme.palette.textPrimary
        ),
        cursorBrush = SolidColor(SidePanelTheme.palette.textPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SidePanelTheme.palette.surfaceBase)
            .border(1.dp, SidePanelTheme.palette.lineSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    )
}

@Composable
private fun SidePanelActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (primary) SidePanelTheme.palette.controlStrong
    else SidePanelTheme.palette.surfaceSubtle.copy(alpha = 0.8f)
    val fg = if (primary) SidePanelTheme.palette.controlStrongText
    else SidePanelTheme.palette.textPrimary
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(),
            style = SidePanelTheme.typography.chipLabel,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
