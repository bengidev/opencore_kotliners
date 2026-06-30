package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.home.theme.HomeTheme
import java.util.UUID

@Composable
internal fun ChatComposerAttachmentsStripView(
    attachments: List<ChatMessageAttachment>,
    onRemove: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    val palette = HomeTheme.palette
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.id }) { attachment ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceSubtle.copy(alpha = if (palette.isDark) 0.55f else 0.9f))
                    .padding(6.dp),
            ) {
                when (attachment.kind) {
                    ChatMessageAttachmentKind.IMAGE -> {
                        attachment.thumbnailJpegData?.let { data ->
                            val bitmap = remember(attachment.id, data) {
                                BitmapFactory.decodeByteArray(data, 0, data.size)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = attachment.filename,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                    ChatMessageAttachmentKind.VIDEO -> {
                        Text(
                            text = attachment.filename,
                            style = HomeTheme.typography.chipLabel,
                            color = palette.textSecondary,
                            modifier = Modifier
                                .width(72.dp)
                                .padding(8.dp),
                            maxLines = 2,
                        )
                    }
                    ChatMessageAttachmentKind.FILE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = palette.textSecondary)
                            Text(
                                text = attachment.filename,
                                style = HomeTheme.typography.chipLabel,
                                color = palette.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                    ChatMessageAttachmentKind.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = palette.accentPrimary)
                            Text(
                                text = "Voice note",
                                style = HomeTheme.typography.chipLabel,
                                color = palette.textSecondary,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onRemove(attachment.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
