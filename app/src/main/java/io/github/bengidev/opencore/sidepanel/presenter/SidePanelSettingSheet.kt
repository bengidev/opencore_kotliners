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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SidePanelSettingSheet(
    component: SidePanelSettingComponent,
    onDismiss: () -> Unit
) {
    val state by component.state.subscribeAsState()
    val palette = HomeTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { component.onAppear() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", color = palette.textPrimary)
                }
            }

            ProviderPicker(
                selectedProviderId = state.selectedProviderId,
                onProviderSelected = component::selectProvider
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Provider API key",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                Text(
                    text = if (state.hasStoredKey) {
                        "A key is stored securely. Enter a new value to replace it."
                    } else {
                        "Add your ${SidePanelProviderApi.resolve(state.selectedProviderId).displayName} API key to enable sending."
                    },
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = palette.surfaceRaised.copy(alpha = if (palette.isDark) 0.5f else 0.85f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = palette.lineSoft.copy(alpha = if (palette.isDark) 0.45f else 0.6f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = palette.textTertiary
                    )
                    TextField(
                        value = state.draftApiKey,
                        onValueChange = component::onDraftChanged,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings-api-key-field"),
                        placeholder = { Text("sk-...") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = exposedTextFieldColors()
                    )
                }
                if (state.hasStoredKey) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("settings-key-stored")
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
                    modifier = Modifier.testTag("settings-error")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = component::save,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings-save-button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.canSave) palette.controlStrong else palette.surfaceSubtle.copy(alpha = 0.9f),
                        contentColor = if (state.canSave) palette.controlStrongText else palette.textTertiary
                    )
                ) {
                    Text(if (state.hasStoredKey) "Update key" else "Save key", fontWeight = FontWeight.SemiBold)
                }
                if (state.hasStoredKey) {
                    TextButton(
                        onClick = component::clear,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("settings-clear-button")
                    ) {
                        Text("Remove stored key", color = palette.accentPrimary)
                    }
                }
            }

            if (state.modelSupportsReasoning) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reasoning",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Choose how much effort the model spends reasoning before it answers.",
                        fontSize = 13.sp,
                        color = palette.textSecondary
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings-reasoning-picker")
                    ) {
                        SidePanelReasoningModel.entries.forEachIndexed { index, model ->
                            SegmentedButton(
                                selected = state.reasoningModel == model,
                                onClick = { component.selectReasoningModel(model) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SidePanelReasoningModel.entries.size
                                )
                            ) {
                                Text(model.title)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPicker(
    selectedProviderId: String,
    onProviderSelected: (String) -> Unit
) {
    val palette = HomeTheme.palette
    var expanded by remember { mutableStateOf(false) }
    val selectedProvider = SidePanelProviderApi.resolve(selectedProviderId)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Provider",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary
        )
        Text(
            text = "Choose which AI provider to use. Each provider has its own API key and model catalog.",
            fontSize = 13.sp,
            color = palette.textSecondary
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-provider-picker")
        ) {
            TextField(
                value = selectedProvider.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = exposedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SidePanelProviderApi.all.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.displayName) },
                        onClick = {
                            onProviderSelected(provider.id)
                            expanded = false
                        }
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
        unfocusedIndicatorColor = lineSoft
    )
}
