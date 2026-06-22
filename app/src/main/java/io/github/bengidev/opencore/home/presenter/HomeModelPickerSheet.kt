package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeModelPickerSheet(
    state: HomeState,
    onDismiss: () -> Unit,
    onModelSelected: (SidePanelModel) -> Unit
) {
    if (!state.isModelPickerVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceRaised
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("home-model-picker-sheet")
        ) {
            Text(
                text = "Select model",
                style = typography.welcomeCaption,
                color = palette.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = palette.lineSoft.copy(alpha = 0.4f))
            LazyColumn {
                items(state.availableModels, key = { it.id }) { model ->
                    val selected = model.id == state.selectedModelId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelected(model) }
                            .semantics {
                                contentDescription = model.displayTitle
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("home-model-row-${model.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = model.displayTitle,
                            style = typography.composerBody,
                            color = if (selected) palette.textPrimary else palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = palette.accentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
