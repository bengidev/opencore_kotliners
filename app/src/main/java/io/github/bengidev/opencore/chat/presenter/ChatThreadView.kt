package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.application.ChatStreamingCoalescingPolicy
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.theme.OpenCoreChatTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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
        val palette = ChatTheme.palette
        val typography = ChatTheme.typography

        if (state.isLoadingMessages) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("chat-thread-loading"),
                contentAlignment = Alignment.Center
            ) {
                ChatLoadingIndicatorView()
            }
            return@OpenCoreChatTheme
        }

        if (state.messages.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("chat-thread-empty"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start a conversation",
                    style = typography.systemMessage,
                    color = palette.systemMessageText
                )
            }
            return@OpenCoreChatTheme
        }

        val listState = rememberLazyListState()
        val lastAssistantTextId = state.messages.lastOrNull {
            it.role == ChatMessageRole.ASSISTANT && it.kind == SidePanelMessageKind.TEXT
        }?.id
        val pendingByteCount = maxOf(
            state.currentPartialText.encodeToByteArray().size,
            state.currentPartialThinking.encodeToByteArray().size
        )

        LaunchedEffect(state.messages.size, awaitingAssistantReply) {
            val targetIndex = state.messages.lastIndex + if (awaitingAssistantReply) 1 else 0
            scrollThreadToBottom(listState, targetIndex, animate = true)
        }

        LaunchedEffect(state.streamingRevision, pendingByteCount) {
            if (state.streamingRevision == 0) return@LaunchedEffect
            val targetIndex = state.messages.lastIndex + if (awaitingAssistantReply) 1 else 0
            val delayMs = ChatStreamingCoalescingPolicy.scrollDelayMs(pendingByteCount)
            if (delayMs > 0L) delay(delayMs)
            scrollThreadToBottom(listState, targetIndex, animate = false)
        }

        LaunchedEffect(state.streamingStatus) {
            val targetIndex = state.messages.lastIndex + if (awaitingAssistantReply) 1 else 0
            scrollThreadToBottom(listState, targetIndex, animate = true)
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
                    message.kind == SidePanelMessageKind.TEXT &&
                    message.role == ChatMessageRole.ASSISTANT
                ChatMessageRowView(
                    message = message,
                    isLastAssistantMessage = message.id == lastAssistantTextId,
                    isStreamingAssistant = isStreamingAssistant,
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

/** Bottom-anchor scroll for the message list. */
private suspend fun scrollThreadToBottom(
    listState: LazyListState,
    targetIndex: Int,
    animate: Boolean
) {
    if (targetIndex < 0) return
    snapshotFlow { listState.layoutInfo.totalItemsCount }
        .filter { it > targetIndex }
        .first()
    try {
        if (animate) {
            listState.animateScrollToItem(targetIndex, scrollOffset = Int.MAX_VALUE)
        } else {
            listState.scrollToItem(targetIndex, scrollOffset = Int.MAX_VALUE)
        }
    } catch (_: IllegalArgumentException) {
        // ponytail: layout race during rapid stream updates — safe to ignore
    }
}
