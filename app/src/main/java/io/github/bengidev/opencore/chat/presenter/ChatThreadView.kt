package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.domain.ChatMessageKind
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.theme.OpenCoreChatTheme
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun ChatThreadView(
    state: ChatState,
    onDismissKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val awaitingAssistantReply =
        state.isSending &&
            state.messages.lastOrNull()?.role == ChatMessageRole.USER &&
            state.streamingThinkingId == null &&
            state.streamingAnswerId == null

    OpenCoreChatTheme {
        val palette = HomeTheme.palette
        val typography = HomeTheme.typography

        if (state.messages.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("chat-thread-empty"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start a conversation",
                    style = typography.welcomeCaption,
                    color = palette.textSecondary
                )
            }
            return@OpenCoreChatTheme
        }

        val listState = rememberLazyListState()
        val lastAssistantTextId = state.messages.lastOrNull {
            it.role == ChatMessageRole.ASSISTANT && it.kind == ChatMessageKind.TEXT
        }?.id

        LaunchedEffect(
            state.messages.size,
            state.currentPartialText.length,
            state.currentPartialThinking.length,
            awaitingAssistantReply
        ) {
            val lastIndex = state.messages.lastIndex + if (awaitingAssistantReply) 1 else 0
            if (lastIndex >= 0) {
                listState.scrollToItem(lastIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("chat-thread-list"),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                val isLastMessage = message.id == state.messages.lastOrNull()?.id
                val isStreamingAssistant = state.isSending &&
                    isLastMessage &&
                    message.kind == ChatMessageKind.TEXT &&
                    message.role == ChatMessageRole.ASSISTANT
                ChatMessageRowView(
                    message = message,
                    isLastAssistantMessage = message.id == lastAssistantTextId,
                    isStreamingAssistant = isStreamingAssistant,
                    streamingStatus = state.streamingStatus,
                    streamErrorMessage = state.streamErrorMessage,
                    onDismissKeyboard = onDismissKeyboard
                )
            }
            if (awaitingAssistantReply) {
                item(key = "chat-loading-indicator") {
                    ChatLoadingIndicatorView()
                }
            }
        }
    }
}
