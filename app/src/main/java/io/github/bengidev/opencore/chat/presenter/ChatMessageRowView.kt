package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.infrastructure.attachments
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val OppositeSideMinWidthDp = 60
private val UserBubbleCorner = RoundedCornerShape(20.dp)

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** One row in the chat thread. */
@Composable
internal fun ChatMessageRowView(
    message: SidePanelMessage,
    isLastAssistantMessage: Boolean,
    isStreamingAssistant: Boolean = false,
    hasCompetingStream: Boolean = false,
    voicePlaybackController: ChatVoiceNotePlaybackController,
    onDismissKeyboard: () -> Unit,
    onReasoningCollapsed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        message.kind == SidePanelMessageKind.THINKING -> ThinkingRow(
            message,
            hasCompetingStream,
            onDismissKeyboard,
            onReasoningCollapsed,
            modifier,
        )
        message.kind == SidePanelMessageKind.OUTPUT_STREAM -> OutputStreamRow(message, onDismissKeyboard, modifier)
        message.role == ChatMessageRole.USER -> UserRow(message, voicePlaybackController, onDismissKeyboard, modifier)
        message.role == ChatMessageRole.ASSISTANT -> AssistantRow(
            message = message,
            isLastAssistantMessage = isLastAssistantMessage,
            isStreamingAssistant = isStreamingAssistant,
            onDismissKeyboard = onDismissKeyboard,
            modifier = modifier
        )
        else -> SystemRow(message.content, onDismissKeyboard, modifier)
    }
}

@Composable
private fun ThinkingRow(
    message: SidePanelMessage,
    hasCompetingStream: Boolean = false,
    onDismissKeyboard: () -> Unit,
    onReasoningCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnTap(onDismissKeyboard)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        ChatReasoningCardView(
            content = message.content,
            isComplete = message.isComplete,
            isStreaming = !message.isComplete,
            hasCompetingStream = hasCompetingStream,
            onCollapsed = onReasoningCollapsed,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 540.dp)
        )
        Spacer(modifier = Modifier.widthIn(min = OppositeSideMinWidthDp.dp))
    }
}

@Composable
private fun OutputStreamRow(
    message: SidePanelMessage,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = ChatOutputStreamDetailCodec.decode(message.detailJson, message.isComplete)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnTap(onDismissKeyboard)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        ChatOutputStreamCardView(
            message = message,
            detail = detail,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 540.dp),
        )
        Spacer(modifier = Modifier.widthIn(min = OppositeSideMinWidthDp.dp))
    }
}

@Composable
private fun UserRow(
    message: SidePanelMessage,
    voicePlaybackController: ChatVoiceNotePlaybackController,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography
    val attachments = message.attachments()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnTap(onDismissKeyboard)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("chat-message-row"),
        horizontalArrangement = Arrangement.End
    ) {
        Spacer(modifier = Modifier.widthIn(min = OppositeSideMinWidthDp.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (attachments.isNotEmpty() || message.content.isNotEmpty()) {
                ChatUserMessageBubbleView(
                    visibleText = message.content,
                    attachments = attachments,
                    playbackController = voicePlaybackController,
                    modifier = Modifier.testTag("chat-message-bubble-user"),
                )
            }
            Text(
                text = formatTime(message),
                style = typography.messageMeta,
                color = palette.messageMetaText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AssistantRow(
    message: SidePanelMessage,
    isLastAssistantMessage: Boolean,
    isStreamingAssistant: Boolean,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnTap(onDismissKeyboard)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("chat-message-row"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (message.content.isNotEmpty()) {
            ChatAssistantTextView(
                text = message.content,
                isStreaming = isStreamingAssistant,
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .align(Alignment.Start)
                    .testTag("chat-message-text")
            )
        }
        if (isLastAssistantMessage) {
            when {
                !isStreamingAssistant && message.content.isNotEmpty() -> Text(
                    text = formatTime(message),
                    style = typography.messageMeta,
                    color = palette.messageMetaText
                )
            }
        }
    }
}

@Composable
private fun SystemRow(
    content: String,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ChatTheme.palette
    val typography = ChatTheme.typography

    Text(
        text = content,
        style = typography.systemMessage,
        color = palette.systemMessageText,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .dismissKeyboardOnTap(onDismissKeyboard)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

private fun formatTime(message: SidePanelMessage): String =
    TimeFormatter.format(message.createdAt.atZone(ZoneId.systemDefault()))

private fun Modifier.dismissKeyboardOnTap(onDismissKeyboard: () -> Unit): Modifier =
    pointerInput(onDismissKeyboard) {
        detectTapGestures { onDismissKeyboard() }
    }
