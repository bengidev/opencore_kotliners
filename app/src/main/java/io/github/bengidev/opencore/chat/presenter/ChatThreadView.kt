package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.application.ChatStreamingCoalescingPolicy
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.theme.OpenCoreChatTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private const val HISTORY_RESTORE_SCROLL_DELAY_MS = 50L

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatThreadView(
    state: ChatState,
    onDismissKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
        val bottomTargetIndex = state.messages.lastIndex
        val imeVisible = WindowInsets.isImeVisible
        val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
        var previousMessageCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(
            state.messages.size,
            state.streamingRevision,
            state.streamingStatus,
            imeVisible,
            imeBottomPx,
        ) {
            if (imeVisible && imeBottomPx <= 0) return@LaunchedEffect
            val isBulkRestore = ChatThreadScrollPolicy.isBulkRestore(
                previousMessageCount = previousMessageCount,
                messageCount = state.messages.size,
            )
            previousMessageCount = state.messages.size
            val animate = ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = isBulkRestore,
                streamingRevision = state.streamingRevision,
                imeVisible = imeVisible,
            )
            if (isBulkRestore) {
                delay(HISTORY_RESTORE_SCROLL_DELAY_MS)
            } else if (state.streamingRevision > 0) {
                val delayMs = ChatStreamingCoalescingPolicy.scrollDelayMs(pendingByteCount)
                if (delayMs > 0L) delay(delayMs)
            }
            withFrameNanos { }
            scrollThreadToBottom(listState, bottomTargetIndex, animate = animate)
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
            items(
                items = state.messages,
                key = ChatThreadItemKeyPolicy::keyFor,
            ) { message ->
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
    snapshotFlow {
        listState.layoutInfo.totalItemsCount to listState.isScrollInProgress
    }
        .filter { (count, scrolling) ->
            count > targetIndex && !ChatThreadScrollPolicy.shouldDeferForActiveScroll(scrolling)
        }
        .first()
    try {
        if (animate) {
            listState.animateScrollToItem(targetIndex, scrollOffset = Int.MAX_VALUE)
        } else {
            listState.scrollToItem(targetIndex, scrollOffset = Int.MAX_VALUE)
        }
    } catch (_: IllegalArgumentException) {
        // Layout race during rapid stream updates — safe to ignore.
    } catch (_: IllegalStateException) {
        // Concurrent user drag or pending scroll — safe to ignore.
    }
}
