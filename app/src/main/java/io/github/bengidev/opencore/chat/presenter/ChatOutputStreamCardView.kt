package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatOutputStreamHumanizer
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

private val CardShape = RoundedCornerShape(14.dp)

/** Inline command output stream row with expandable detail sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatOutputStreamCardView(
    message: SidePanelMessage,
    detail: ChatOutputStreamDetail,
    modifier: Modifier = Modifier,
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    val corePalette = ChatTheme.corePalette
    var isShowingDetailSheet by remember { mutableStateOf(false) }

    val isRunning = detail.status == ChatOutputStreamStatus.RUNNING && !message.isComplete
    val display = ChatOutputStreamHumanizer.humanize(message.content, isRunning)
    val statusLabel = when (detail.status) {
        ChatOutputStreamStatus.RUNNING -> "running"
        ChatOutputStreamStatus.COMPLETED -> "completed"
        ChatOutputStreamStatus.FAILED -> "failed"
    }
    val statusColor = when (detail.status) {
        ChatOutputStreamStatus.RUNNING -> palette.streamingDot
        ChatOutputStreamStatus.COMPLETED -> palette.reasoningText
        ChatOutputStreamStatus.FAILED -> palette.errorIcon
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(palette.reasoningCard)
            .border(0.5.dp, palette.reasoningBorder, CardShape)
            .clickable { isShowingDetailSheet = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("chat-output-stream-card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${display.verb} ${display.target}",
                style = typography.reasoningHeader,
                color = palette.reasoningText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.widthIn(min = 6.dp))
            Text(
                text = statusLabel,
                style = typography.messageMeta,
                color = statusColor.copy(
                    alpha = if (detail.status == ChatOutputStreamStatus.FAILED) 1f else 0.5f
                ),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.messageMetaText.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    if (isShowingDetailSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { isShowingDetailSheet = false },
            sheetState = sheetState,
            containerColor = corePalette.surfaceRaised,
        ) {
            ChatOutputStreamDetailSheet(
                message = message,
                detail = detail,
                display = display,
                statusLabel = statusLabel,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ChatOutputStreamDetailSheet(
    message: SidePanelMessage,
    detail: ChatOutputStreamDetail,
    display: ChatOutputStreamHumanizer.Info,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    val corePalette = ChatTheme.corePalette
    val typography = ChatTheme.typography
    var isOutputExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = corePalette.accentPrimary,
                )
                Text(
                    text = "Command",
                    style = typography.messageMeta.copy(fontFamily = FontFamily.Monospace),
                    color = corePalette.accentPrimary,
                )
            }
            Text(
                text = message.content,
                style = typography.reasoningBody.copy(fontFamily = FontFamily.Monospace),
                color = corePalette.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(corePalette.surfaceBase)
                    .padding(12.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetadataRow(label = "Action", value = "${display.verb} ${display.target}")
            detail.cwd?.takeIf { it.isNotEmpty() }?.let { cwd ->
                MetadataRow(label = "Directory", value = cwd)
            }
            detail.exitCode?.let { exitCode ->
                MetadataRow(
                    label = "Exit code",
                    value = exitCode.toString(),
                    valueColor = if (exitCode == 0) corePalette.success else corePalette.danger,
                )
            }
            detail.durationMs?.let { durationMs ->
                MetadataRow(label = "Duration", value = formatDuration(durationMs))
            }
            MetadataRow(label = "Status", value = statusLabel)
        }

        if (detail.outputTail.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clickable { isOutputExpanded = !isOutputExpanded }
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isOutputExpanded) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = null,
                        tint = corePalette.textSecondary,
                    )
                    Text(
                        text = "Output (last ${ChatOutputStreamDetail.MAX_OUTPUT_LINES} lines)",
                        style = typography.messageMeta.copy(fontFamily = FontFamily.Monospace),
                        color = corePalette.textSecondary,
                    )
                }
                if (isOutputExpanded) {
                    Text(
                        text = detail.outputTail,
                        style = typography.messageMeta.copy(fontFamily = FontFamily.Monospace),
                        color = corePalette.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(corePalette.surfaceBase)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val corePalette = ChatTheme.corePalette
    val typography = ChatTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.messageMeta.copy(fontFamily = FontFamily.Monospace),
            color = corePalette.textSecondary,
        )
        Text(
            text = value,
            style = typography.messageMeta.copy(fontFamily = FontFamily.Monospace),
            color = valueColor ?: corePalette.textPrimary,
        )
    }
}

private fun formatDuration(ms: Int): String {
    if (ms < 1000) return "${ms}ms"
    val seconds = ms / 1000.0
    if (seconds < 60) return String.format("%.1fs", seconds)
    val minutes = seconds.toInt() / 60
    val remainingSeconds = seconds.toInt() % 60
    return "${minutes}m ${remainingSeconds}s"
}
