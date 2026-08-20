package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add attachment",
                style = typography.composerBody.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
            )
        },
        text = {
            Text(
                text = attachmentMenuMessage(options),
                style = typography.chipLabel,
                color = palette.textSecondary,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = palette.textSecondary)
            }
        },
        confirmButton = {
            options.forEach { option ->
                TextButton(onClick = { onOptionSelected(option) }) {
                    Text(
                        text = option.label,
                        color = palette.textPrimary,
                        style = typography.chipLabel.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        },
        shape = shape,
        containerColor = palette.surfaceRaised,
    )
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
