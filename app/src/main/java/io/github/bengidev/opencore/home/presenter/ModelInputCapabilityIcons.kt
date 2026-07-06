package io.github.bengidev.opencore.home.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun ModelInputCapabilityIcons(
    supportsImageInput: Boolean,
    supportsVideoInput: Boolean,
    supportsFileInput: Boolean,
    modelId: String,
    modifier: Modifier = Modifier,
) {
    if (!supportsImageInput && !supportsVideoInput && !supportsFileInput) return

    val palette = HomeTheme.palette

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (supportsImageInput) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Supports image input",
                tint = palette.textTertiary,
                modifier = Modifier
                    .size(14.dp)
                    .testTag("home-model-capability-image-$modelId"),
            )
        }
        if (supportsVideoInput) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Supports video input",
                tint = palette.textTertiary,
                modifier = Modifier
                    .size(14.dp)
                    .testTag("home-model-capability-video-$modelId"),
            )
        }
        if (supportsFileInput) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Supports file input",
                tint = palette.textTertiary,
                modifier = Modifier
                    .size(14.dp)
                    .testTag("home-model-capability-file-$modelId"),
            )
        }
    }
}
