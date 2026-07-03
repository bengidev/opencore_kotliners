package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingState
import io.github.bengidev.opencore.shared.providers.ProviderRegistry

@Composable
internal fun SidePanelSettingContent(
    state: SidePanelSettingState,
    onDismiss: (() -> Unit)?,
    onProviderSelected: (String) -> Unit,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HomeTheme.palette

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            onDismiss?.let {
                TextButton(onClick = it) {
                    Text("Done", color = palette.textPrimary)
                }
            }
        }

        ProviderPicker(
            selectedProviderId = state.selectedProviderId,
            onProviderSelected = onProviderSelected,
        )

        val selectedProvider = ProviderRegistry.resolve(state.selectedProviderId).descriptor
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = selectedProvider.credentialLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Text(
                text = if (state.hasStoredKey) {
                    "A ${selectedProvider.credentialLabel} is stored securely. Enter a new value to replace it."
                } else {
                    selectedProvider.credentialPrompt
                },
                fontSize = 13.sp,
                color = palette.textSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = palette.surfaceRaised.copy(alpha = if (palette.isDark) 0.5f else 0.85f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = palette.lineSoft.copy(alpha = if (palette.isDark) 0.45f else 0.6f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = palette.textTertiary,
                )
                TextField(
                    value = state.draftApiKey,
                    onValueChange = onDraftChanged,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings-api-key-field"),
                    placeholder = { Text(selectedProvider.credentialPlaceholder) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = exposedTextFieldColors(),
                )
            }
            if (state.hasStoredKey) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("settings-key-stored"),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.height(14.dp))
                    Text("Key stored", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.textSecondary)
                }
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = palette.accentPrimary,
                modifier = Modifier.testTag("settings-error"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings-save-button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.canSave) palette.controlStrong else palette.surfaceSubtle.copy(alpha = 0.9f),
                    contentColor = if (state.canSave) palette.controlStrongText else palette.textTertiary,
                ),
            ) {
                Text(if (state.hasStoredKey) "Update key" else "Save key", fontWeight = FontWeight.SemiBold)
            }
            if (state.hasStoredKey) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings-clear-button"),
                ) {
                    Text("Remove stored key", color = palette.accentPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPicker(
    selectedProviderId: String,
    onProviderSelected: (String) -> Unit,
) {
    val palette = HomeTheme.palette
    var expanded by remember { mutableStateOf(false) }
    val selectedProvider = ProviderRegistry.resolve(selectedProviderId).descriptor

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Provider",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
        )
        Text(
            text = "Choose which AI provider to use. Each provider has its own API key and model catalog.",
            fontSize = 13.sp,
            color = palette.textSecondary,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-provider-picker"),
        ) {
            TextField(
                value = selectedProvider.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = exposedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ProviderRegistry.allDescriptors.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.displayName) },
                        onClick = {
                            onProviderSelected(provider.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun exposedTextFieldColors() = with(HomeTheme.palette) {
    androidx.compose.material3.TextFieldDefaults.colors(
        focusedContainerColor = surfaceRaised.copy(alpha = if (isDark) 0.5f else 0.85f),
        unfocusedContainerColor = surfaceRaised.copy(alpha = if (isDark) 0.5f else 0.85f),
        focusedIndicatorColor = lineSoft,
        unfocusedIndicatorColor = lineSoft,
    )
}
