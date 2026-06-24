package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeModelPickerSheet(
    state: HomeState,
    onDismiss: () -> Unit,
    onModelSelected: (SidePanelModel) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterFreeOnlyChanged: (Boolean) -> Unit
) {
    if (!state.isModelPickerVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val showFreeFilter = state.selectedProviderId == SidePanelProviderApi.openRouter.id

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

            ModelSearchBar(
                query = state.modelSearchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (showFreeFilter) {
                ModelFilterBar(
                    filterFreeOnly = state.modelFilterFreeOnly,
                    modelCount = state.filteredModels.size,
                    onFilterFreeOnlyChanged = onFilterFreeOnlyChanged,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            state.modelCatalogErrorHint?.let { hint ->
                Text(
                    text = hint,
                    style = typography.chipLabel,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider(color = palette.lineSoft.copy(alpha = 0.4f))

            when {
                state.isLoadingModels -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("home-model-picker-loading"),
                            color = palette.accentPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                state.filteredModels.isEmpty() -> {
                    ModelPickerEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp)
                    )
                }
                else -> {
                    LazyColumn {
                        items(state.filteredModels, key = { it.id }) { model ->
                            HomeModelPickerRow(
                                model = model,
                                selected = model.id == state.selectedModelId,
                                onClick = { onModelSelected(model) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceSubtle.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(16.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .testTag("home-model-search"),
            textStyle = HomeTheme.typography.composerBody.copy(color = palette.textPrimary),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search models…",
                        style = HomeTheme.typography.composerBody,
                        color = palette.textTertiary
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChanged("") },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = palette.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelFilterBar(
    filterFreeOnly: Boolean,
    modelCount: Int,
    onFilterFreeOnlyChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (filterFreeOnly) {
                        palette.accentSoft.copy(alpha = 0.9f)
                    } else {
                        palette.surfaceSubtle.copy(alpha = 0.7f)
                    }
                )
                .clickable { onFilterFreeOnlyChanged(!filterFreeOnly) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("home-model-free-filter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (filterFreeOnly) palette.accentPrimary else palette.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Free only",
                style = HomeTheme.typography.chipLabel.copy(fontSize = 13.sp),
                color = if (filterFreeOnly) palette.accentPrimary else palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$modelCount models",
            style = HomeTheme.typography.chipLabel.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = palette.textTertiary
        )
    }
}

@Composable
private fun HomeModelPickerRow(
    model: SidePanelModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) palette.accentSoft.copy(alpha = 0.06f) else palette.surfaceRaised
            )
            .clickable(onClick = onClick)
            .semantics {
                val freeLabel = if (model.isFree) ", free" else ""
                val selectedLabel = if (selected) ", selected" else ""
                contentDescription = "${model.displayTitle}$freeLabel$selectedLabel"
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("home-model-row-${model.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        palette.accentPrimary.copy(alpha = 0.15f)
                    } else {
                        palette.surfaceSubtle.copy(alpha = 0.7f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.Check else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (selected) palette.accentPrimary else palette.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = model.displayTitle,
                    style = typography.composerBody.copy(fontWeight = FontWeight.Medium),
                    color = palette.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (model.isFree) {
                    Text(
                        text = "FREE",
                        style = typography.contextUsage.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = palette.accentPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(palette.accentSoft.copy(alpha = 0.9f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("home-model-free-badge-${model.id}")
                    )
                }
            }
            model.contextLength?.let { contextLength ->
                Text(
                    text = contextLengthLabel(contextLength),
                    style = typography.chipLabel.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = palette.textTertiary
                )
            }
        }
    }
}

@Composable
private fun ModelPickerEmptyState(modifier: Modifier = Modifier) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "No models found",
            style = typography.composerBody.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary
        )
        Text(
            text = "Try a different search term or remove the free-only filter.",
            style = typography.chipLabel,
            color = palette.textSecondary
        )
    }
}

private fun contextLengthLabel(tokens: Int): String {
    val count = tokens.toDouble()
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000).toInt()}M ctx"
        count >= 1_000 -> "${(count / 1_000).toInt()}K ctx"
        else -> "$tokens ctx"
    }
}
