package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatVoiceNotePlaybackDisplayLogic
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingDisplayLogic

@Composable
internal fun ChatUserMessageBubbleView(
    visibleText: String,
    attachments: List<ChatMessageAttachment>,
    playbackController: ChatVoiceNotePlaybackController,
    modifier: Modifier = Modifier,
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    val playbackState by playbackController.playbackState.collectAsState()
    val playbackCurrentTime by playbackController.playbackCurrentTime.collectAsState()
    val playbackError by playbackController.lastErrorMessage.collectAsState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.userBubble)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
                ChatMessageAttachmentKind.VIDEO -> {
                    Text(
                        text = attachment.filename,
                        style = typography.userMessageBody,
                        color = palette.userBubbleText,
                    )
                }
                ChatMessageAttachmentKind.FILE -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = palette.userBubbleText)
                        Text(
                            text = attachment.filename,
                            style = typography.userMessageBody,
                            color = palette.userBubbleText,
                        )
                    }
                }
                ChatMessageAttachmentKind.AUDIO -> {
                    val isPlaying = playbackState is ChatVoiceNotePlaybackController.PlaybackState.Playing &&
                        (playbackState as ChatVoiceNotePlaybackController.PlaybackState.Playing).attachmentId == attachment.id
                    val isPlaybackActive = playbackController.isActive(attachment.id)
                    val totalDuration = playbackController.resolvedDurationSeconds(attachment)
                    val progress = if (isPlaybackActive) {
                        ChatVoiceNotePlaybackDisplayLogic.playbackProgress(
                            currentTime = playbackCurrentTime,
                            duration = totalDuration,
                        )
                    } else {
                        0.0
                    }
                    val displayed = ChatVoiceNotePlaybackDisplayLogic.displayedDuration(
                        currentTime = playbackCurrentTime,
                        totalDuration = totalDuration,
                        isPlaybackActive = isPlaybackActive,
                    )
                    val waveformHeights = SpeechRecordingDisplayLogic.playbackWaveformBarHeights(
                        levels = attachment.waveformSamples,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { playbackController.toggle(attachment) }
                                .padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                                tint = palette.userBubbleText,
                                modifier = Modifier.size(22.dp),
                            )
                            ChatWaveformBarsView(
                                heights = waveformHeights,
                                progress = progress,
                                showsPlaybackProgress = isPlaybackActive,
                                activeColor = palette.userBubbleText,
                                idleColor = palette.userBubbleText.copy(alpha = 0.35f),
                                unplayedColor = palette.userBubbleText.copy(alpha = 0.22f),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = SpeechRecordingDisplayLogic.formatElapsedDuration(displayed),
                                style = typography.messageMeta,
                                color = palette.userBubbleText.copy(alpha = 0.85f),
                            )
                        }
                        playbackError?.let { message ->
                            Text(
                                text = message,
                                style = typography.messageMeta,
                                color = palette.userBubbleText.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
        if (visibleText.isNotEmpty()) {
            Text(
                text = visibleText,
                style = typography.userMessageBody,
                color = palette.userBubbleText,
            )
        }
    }
}
