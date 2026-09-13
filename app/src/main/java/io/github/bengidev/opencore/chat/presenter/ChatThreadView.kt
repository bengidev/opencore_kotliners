package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
private const val IME_LAYOUT_SCROLL_DELAY_MS = 48L
private const val STREAM_FINAL_LAYOUT_SCROLL_DELAY_MS = 64L

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatThreadView(
    state: ChatState,
    voicePlaybackController: ChatVoiceNotePlaybackController,
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
        val pendingByteCount = maxOf(
            state.currentPartialText.encodeToByteArray().size,
            state.currentPartialThinking.encodeToByteArray().size,
            state.streamingOutputStreamId?.let { 1 } ?: 0,
        )
        val displayMessages = ChatThreadLayoutPolicy.displayOrder(state.messages)
        val bottomTargetIndex = ChatThreadLayoutPolicy.tailScrollIndex(displayMessages)
        val lastAssistantTextId = displayMessages.lastOrNull {
            it.role == ChatMessageRole.ASSISTANT && it.kind == SidePanelMessageKind.TEXT
        }?.id
        val imeVisible = WindowInsets.isImeVisible
        val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
        var previousMessageCount by remember { mutableIntStateOf(0) }
        var previousImeBottomPx by remember { mutableIntStateOf(0) }
        var previousIsSending by remember { mutableStateOf(state.isSending) }
        var lastScrolledByteCount by remember { mutableIntStateOf(-1) }
        var scrollToBottomRequest by remember { mutableLongStateOf(0L) }
        val lastMessageContentLength = displayMessages.lastOrNull()?.content?.length ?: 0

        LaunchedEffect(scrollToBottomRequest, bottomTargetIndex) {
            if (scrollToBottomRequest == 0L) return@LaunchedEffect
            withFrameNanos { }
            scrollThreadToBottom(
                listState,
                bottomTargetIndex,
                animate = ChatThreadScrollPolicy.shouldAnimateReasoningCollapseScroll(),
            )
        }

        LaunchedEffect(
            state.messages.size,
            state.streamingRevision,
            state.streamingStatus,
            state.isSending,
            imeVisible,
            imeBottomPx,
            pendingByteCount,
            lastMessageContentLength,
        ) {
            val messageCount = state.messages.size
            val isStreaming = state.streamingRevision > 0
            val streamFinished = ChatThreadScrollPolicy.shouldScrollForStreamFinished(
                wasSending = previousIsSending,
                isSending = state.isSending,
            )
            val newMessageAdded = ChatThreadScrollPolicy.shouldScrollForNewMessage(
                previousMessageCount = previousMessageCount,
                messageCount = messageCount,
            )
            val imeChanged = ChatThreadScrollPolicy.shouldScrollForImeChange(
                imeBottomPx = imeBottomPx,
                previousImeBottomPx = previousImeBottomPx,
            )
            val shouldScroll = when {
                streamFinished -> true
                newMessageAdded -> true
                imeChanged -> true
                isStreaming -> ChatThreadScrollPolicy.shouldScrollForStreamingUpdate(
                    pendingByteCount = pendingByteCount,
                    lastScrolledByteCount = lastScrolledByteCount,
                )
                else -> true
            }
            if (!shouldScroll) return@LaunchedEffect

            val isBulkRestore = ChatThreadScrollPolicy.isBulkRestore(
                previousMessageCount = previousMessageCount,
                messageCount = messageCount,
            )
            val animate = ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = isBulkRestore,
                streamingRevision = state.streamingRevision,
                imeVisible = imeVisible,
                previousMessageCount = previousMessageCount,
            )
            previousMessageCount = messageCount
            previousImeBottomPx = imeBottomPx
            previousIsSending = state.isSending
            if (isStreaming) {
                lastScrolledByteCount = pendingByteCount
            } else {
                lastScrolledByteCount = -1
            }
            when {
                isBulkRestore -> delay(HISTORY_RESTORE_SCROLL_DELAY_MS)
                streamFinished -> delay(STREAM_FINAL_LAYOUT_SCROLL_DELAY_MS)
                imeChanged && ChatThreadScrollPolicy.shouldDelayForImeLayout(imeBottomPx) -> {
                    delay(IME_LAYOUT_SCROLL_DELAY_MS)
                }
                isStreaming -> {
                    val delayMs = ChatStreamingCoalescingPolicy.scrollDelayMs(pendingByteCount)
                    if (delayMs > 0L) delay(delayMs)
                }
            }
            withFrameNanos { }
            scrollThreadToBottom(listState, bottomTargetIndex, animate = animate)
            if (streamFinished) {
                withFrameNanos { }
                delay(STREAM_FINAL_LAYOUT_SCROLL_DELAY_MS)
                withFrameNanos { }
                scrollThreadToBottom(listState, bottomTargetIndex, animate = false)
            }
            if (imeChanged && imeBottomPx > 0) {
                withFrameNanos { }
                scrollThreadToBottom(listState, bottomTargetIndex, animate = false)
            }
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("chat-thread-list"),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(ChatThreadLayoutPolicy.listAlignment)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                reverseLayout = ChatThreadLayoutPolicy.useReverseLayout(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = ChatThreadLayoutPolicy.contentPadding(),
            ) {
                val hasCompetingStream = ChatCompetingStreamPolicy.hasCompetingStream(state)
                items(
                    items = displayMessages,
                    key = ChatThreadItemKeyPolicy::keyFor,
                ) { message ->
                    val isStreamingAssistant = state.isSending &&
                        message.id == state.streamingAnswerId &&
                        message.kind == SidePanelMessageKind.TEXT &&
                        message.role == ChatMessageRole.ASSISTANT &&
                        !message.isComplete
                    ChatMessageRowView(
                        message = message,
                        isLastAssistantMessage = message.id == lastAssistantTextId,
                        isStreamingAssistant = isStreamingAssistant,
                        hasCompetingStream = hasCompetingStream,
                        voicePlaybackController = voicePlaybackController,
                        onDismissKeyboard = onDismissKeyboard,
                        onReasoningCollapsed = { scrollToBottomRequest++ },
                    )
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
    snapshotFlow {
        listState.layoutInfo.totalItemsCount to listState.isScrollInProgress
    }
        .filter { (count, scrolling) ->
            count > targetIndex && !ChatThreadScrollPolicy.shouldDeferForActiveScroll(scrolling)
        }
        .first()
    try {
        val scrollOffset = ChatThreadLayoutPolicy.tailScrollOffset()
        if (animate) {
            listState.animateScrollToItem(targetIndex, scrollOffset = scrollOffset)
        } else {
            listState.scrollToItem(targetIndex, scrollOffset = scrollOffset)
        }
    } catch (_: IllegalArgumentException) {
        // Layout race during rapid stream updates — safe to ignore.
    } catch (_: IllegalStateException) {
        delay(32L)
        try {
            val scrollOffset = ChatThreadLayoutPolicy.tailScrollOffset()
            if (animate) {
                listState.animateScrollToItem(targetIndex, scrollOffset = scrollOffset)
            } else {
                listState.scrollToItem(targetIndex, scrollOffset = scrollOffset)
            }
        } catch (_: IllegalArgumentException) {
        } catch (_: IllegalStateException) {
            // Concurrent user drag or pending scroll — safe to ignore.
        }
    }
}
