package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.application.HomeState
import io.github.bengidev.opencore.home.presenter.components.homeComposerGlass
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun HomeComposerView(
    state: HomeState,
    isSending: Boolean = false,
    isLoadingMessages: Boolean = false,
    onDraftMessageChanged: (String) -> Unit,
    onAttachmentTapped: () -> Unit,
    onMicrophoneTapped: () -> Unit,
    onSendTapped: () -> Unit,
    onConfigureApiKeyTapped: () -> Unit,
    onModelSelectorTapped: () -> Unit,
    onSpeedModeSelected: (HomeComposerSpeedMode) -> Unit,
    onReasoningEffortSelected: (ModelReasoningEffort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeComposerPromptPanel(
            draftMessage = state.draftMessage,
            canSend = state.canSend && !isSending && !isLoadingMessages,
            showMissingApiKeyHint = state.showMissingApiKeyHint,
            onConfigureApiKeyTapped = onConfigureApiKeyTapped,
            onDraftMessageChanged = onDraftMessageChanged,
            onAttachmentTapped = onAttachmentTapped,
            onMicrophoneTapped = onMicrophoneTapped,
            onSendTapped = onSendTapped
        )

        HomeComposerContextRail(
            state = state,
            onModelSelectorTapped = onModelSelectorTapped,
            onSpeedModeSelected = onSpeedModeSelected,
            onReasoningEffortSelected = onReasoningEffortSelected,
        )
    }
}

@Composable
private fun HomeComposerPromptPanel(
    draftMessage: String,
    canSend: Boolean,
    showMissingApiKeyHint: Boolean,
    onConfigureApiKeyTapped: () -> Unit,
    onDraftMessageChanged: (String) -> Unit,
    onAttachmentTapped: () -> Unit,
    onMicrophoneTapped: () -> Unit,
    onSendTapped: () -> Unit
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .homeComposerGlass(cornerRadius = 28.dp, shadowOpacity = 0.16f)
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showMissingApiKeyHint) {
            MissingApiKeyHint(onClick = onConfigureApiKeyTapped)
        }

        BasicTextField(
            value = draftMessage,
            onValueChange = onDraftMessageChanged,
            textStyle = typography.composerBody.copy(color = palette.textPrimary),
            cursorBrush = SolidColor(palette.accentPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (draftMessage.isEmpty()) {
                        Text(
                            text = "Ask anything... @files, \$skills, /commands",
                            style = typography.composerBody,
                            color = palette.textTertiary
                        )
                    }
                    innerTextField()
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HomeComposerIconButton(
                imageVector = Icons.Default.Add,
                contentDescription = "Add attachment",
                onClick = onAttachmentTapped
            )

            Spacer(modifier = Modifier.weight(1f))

            HomeComposerIconButton(
                imageVector = Icons.Default.Mic,
                contentDescription = "Start voice input",
                onClick = onMicrophoneTapped
            )

            HomeComposerSendButton(
                canSend = canSend,
                onClick = onSendTapped
            )
        }
    }
}

@Composable
private fun MissingApiKeyHint(onClick: () -> Unit) {
    val palette = HomeTheme.palette
    val shape = RoundedCornerShape(14.dp)
    val fill = palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.5f else 0.8f)
    val border = palette.border.copy(alpha = if (palette.isDark) 0.45f else 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(width = 1.dp, color = border, shape = shape)
            .semantics {
                role = Role.Button
                contentDescription = "Add an API key in Settings to start sending"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyOff,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "Add an API key in Settings to start sending",
            style = HomeTheme.typography.chipLabel,
            color = palette.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun HomeComposerContextRail(
    state: HomeState,
    onModelSelectorTapped: () -> Unit,
    onSpeedModeSelected: (HomeComposerSpeedMode) -> Unit,
    onReasoningEffortSelected: (ModelReasoningEffort) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.hasApiKey && !state.isModelCatalogAvailable) {
            state.modelCatalogErrorHint?.let { hint ->
                CatalogUnavailableHint(message = hint)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                HomeComposerModelChip(
                    title = state.modelPickerTitle,
                    enabled = state.isModelCatalogAvailable,
                    onClick = onModelSelectorTapped
                )
                if (state.selectedModelSupportsReasoning) {
                    HomeComposerReasoningChip(
                        selectedEffort = state.selectedReasoningEffort,
                        availableEfforts = state.availableReasoningEfforts,
                        onReasoningEffortSelected = onReasoningEffortSelected,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.selectedModelSupportsSpeedModes) {
                    HomeComposerSpeedChip(
                        speedMode = state.speedMode,
                        onSpeedModeSelected = onSpeedModeSelected
                    )
                }
                HomeComposerContextUsageIndicator(usage = state.contextUsage)
            }
        }
    }
}

@Composable
private fun CatalogUnavailableHint(message: String) {
    val palette = HomeTheme.palette
    Text(
        text = message,
        style = HomeTheme.typography.chipLabel,
        color = palette.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HomeComposerModelChip(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val contentColor = if (enabled) palette.textSecondary else palette.textTertiary

    Row(
        modifier = Modifier
            .widthIn(min = 92.dp)
            .height(30.dp)
            .homeComposerGlass(cornerRadius = 16.dp, shadowOpacity = 0.06f)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            style = typography.chipLabel,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (enabled) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HomeComposerReasoningChip(
    selectedEffort: ModelReasoningEffort,
    availableEfforts: List<ModelReasoningEffort>,
    onReasoningEffortSelected: (ModelReasoningEffort) -> Unit,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    var popoverExpanded by remember { mutableStateOf(false) }

    ComposerControlPopoverHost(
        expanded = popoverExpanded,
        onExpandedChange = { popoverExpanded = it },
        anchor = {
            Row(
                modifier = Modifier
                    .widthIn(min = 92.dp)
                    .height(30.dp)
                    .homeComposerGlass(cornerRadius = 16.dp, shadowOpacity = 0.06f)
                    .clickable { popoverExpanded = true }
                    .padding(horizontal = 10.dp)
                    .semantics {
                        contentDescription = "Reasoning, ${selectedEffort.title}"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = selectedEffort.title,
                    style = typography.chipLabel,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        content = {
            ReasoningControlPopover(
                selectedEffort = selectedEffort,
                availableEfforts = availableEfforts,
                onEffortSelected = { effort ->
                    popoverExpanded = false
                    onReasoningEffortSelected(effort)
                },
            )
        },
    )
}

@Composable
private fun HomeComposerSpeedChip(
    speedMode: HomeComposerSpeedMode,
    onSpeedModeSelected: (HomeComposerSpeedMode) -> Unit
) {
    val palette = HomeTheme.palette
    var popoverExpanded by remember { mutableStateOf(false) }
    val isFast = speedMode == HomeComposerSpeedMode.FAST

    ComposerControlPopoverHost(
        expanded = popoverExpanded,
        onExpandedChange = { popoverExpanded = it },
        anchor = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .homeComposerGlass(cornerRadius = 16.dp, shadowOpacity = 0.06f)
                    .clickable { popoverExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Speed, ${speedMode.title}",
                    tint = if (isFast) palette.accentPrimary else palette.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        },
        content = {
            SpeedControlPopover(
                speedMode = speedMode,
                onSpeedModeSelected = { mode ->
                    popoverExpanded = false
                    onSpeedModeSelected(mode)
                },
            )
        },
    )
}

@Composable
private fun HomeComposerContextUsageIndicator(
    usage: ContextWindowUsage,
) {
    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val usagePercent = usage.percentUsed
    val fraction = (usagePercent / 100f).coerceIn(0f, 1f)
    var popoverExpanded by remember { mutableStateOf(false) }

    ComposerControlPopoverHost(
        expanded = popoverExpanded,
        onExpandedChange = { popoverExpanded = it },
        anchor = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceRaised.copy(alpha = if (palette.isDark) 0.42f else 0.72f))
                    .border(1.dp, palette.accentPrimary.copy(alpha = if (palette.isDark) 0.18f else 0.12f), CircleShape)
                    .clickable { popoverExpanded = true }
                    .semantics {
                        contentDescription = "Context usage $usagePercent percent"
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(23.dp)) {
                    drawCircle(
                        color = palette.accentPrimary.copy(alpha = if (palette.isDark) 0.14f else 0.12f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = palette.accentPrimary.copy(alpha = if (palette.isDark) 0.92f else 0.82f),
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = usagePercent.toString(),
                    style = typography.contextUsage,
                    color = palette.accentPrimary
                )
            }
        },
        content = {
            ContextWindowPopover(usage = usage)
        },
    )
}

@Composable
private fun HomeComposerIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = HomeTheme.palette
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(30.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HomeComposerSendButton(
    canSend: Boolean,
    onClick: () -> Unit
) {
    val palette = HomeTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val circleFill = if (canSend) {
        palette.controlStrong
    } else {
        palette.surfaceSubtle.copy(alpha = 0.9f)
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(circleFill)
            .semantics { contentDescription = "Send message" }
            .clickable(
                enabled = canSend,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = if (canSend) palette.controlStrongText else palette.textTertiary,
            modifier = Modifier.size(17.dp)
        )
    }
}
