package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.home.utilities.HomeComposerModelCapabilityLogic.AttachmentMenuOption

@Composable
internal fun ComposerAttachmentMenuDialog(
    options: List<AttachmentMenuOption>,
    onDismiss: () -> Unit,
    onOptionSelected: (AttachmentMenuOption) -> Unit,
) {
    if (options.isEmpty()) return

    val palette = HomeTheme.palette
    val typography = HomeTheme.typography
    val shape = RoundedCornerShape(20.dp)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(palette.surfaceRaised)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add attachment",
                style = typography.composerBody.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
            )
            Text(
                text = attachmentMenuMessage(options),
                style = typography.chipLabel,
                color = palette.textSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = palette.textSecondary,
                    )
                }
                options.forEach { option ->
                    TextButton(onClick = { onOptionSelected(option) }) {
                        Text(
                            text = option.label,
                            color = palette.textPrimary,
                            style = typography.chipLabel.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}

private val AttachmentMenuOption.label: String
    get() = when (this) {
        AttachmentMenuOption.PhotoLibrary -> "Photo Library"
        AttachmentMenuOption.ImportFile -> "Import File"
    }

private fun attachmentMenuMessage(options: List<AttachmentMenuOption>): String = when {
    options.contains(AttachmentMenuOption.PhotoLibrary) &&
        options.contains(AttachmentMenuOption.ImportFile) ->
        "Attach a photo from your library or import a text file."
    options.contains(AttachmentMenuOption.PhotoLibrary) ->
        "Attach a photo or video from your library."
    else -> "Import a text file to include with your message."
}
