package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.dedupeByThreadItemKey
import java.util.UUID

internal data class ChatState(
    val activeConversation: SidePanelConversation? = null,
    val messages: List<SidePanelMessage> = emptyList(),
    val draftAttachments: List<ChatMessageAttachment> = emptyList(),
    val isLoadingMessages: Boolean = false,
    val isSending: Boolean = false,
    val streamingStatus: ChatStreamingStatus = ChatStreamingStatus.Idle,
    val streamErrorMessage: String? = null,
    val currentPartialText: String = "",
    val currentPartialThinking: String = "",
    val streamingThinkingId: UUID? = null,
    val streamingAnswerId: UUID? = null,
    val streamingOutputStreamId: UUID? = null,
    /** Bumped when batched streaming content is applied to `messages` (scroll anchor). */
    val streamingRevision: Int = 0
) {
    val isThreadActive: Boolean
        get() = activeConversation != null

    /** True while a turn is actively streaming; drives the status capsule above the composer. */
    val showsStreamingStatusCapsule: Boolean
        get() {
            if (!isSending || streamingStatus != ChatStreamingStatus.Running) return false
            val last = messages.lastOrNull() ?: return false
            if (last.role == ChatMessageRole.USER) return true
            return streamingAnswerId != null ||
                streamingThinkingId != null ||
                currentPartialText.isNotEmpty() ||
                currentPartialThinking.isNotEmpty()
        }

    val headerTitle: String
        get() = activeConversation?.title.orEmpty()

    fun toStreamingState(): ChatStreamingState = ChatStreamingState(
        messages = messages,
        currentPartialText = currentPartialText,
        currentPartialThinking = currentPartialThinking,
        streamingThinkingId = streamingThinkingId,
        streamingAnswerId = streamingAnswerId,
        streamingOutputStreamId = streamingOutputStreamId,
        streamingStatus = streamingStatus,
        streamErrorMessage = streamErrorMessage
    )

    fun applyStreamingMerge(
        result: ChatStreamingMergeResult,
        isSending: Boolean,
        bumpStreamingRevision: Boolean = false
    ): ChatState =
        copy(
            messages = result.state.messages.dedupeByThreadItemKey(),
            currentPartialText = result.state.currentPartialText,
            currentPartialThinking = result.state.currentPartialThinking,
            streamingThinkingId = result.state.streamingThinkingId,
            streamingAnswerId = result.state.streamingAnswerId,
            streamingOutputStreamId = result.state.streamingOutputStreamId,
            streamingStatus = result.state.streamingStatus,
            streamErrorMessage = result.state.streamErrorMessage,
            isSending = isSending,
            streamingRevision = if (bumpStreamingRevision) streamingRevision + 1 else streamingRevision
        )
}

internal sealed interface ChatIntent {
    data class DraftAttachmentAdded(val attachment: ChatMessageAttachment) : ChatIntent
    data class DraftAttachmentRemoved(val id: java.util.UUID) : ChatIntent
  /** Clears draft attachments and deletes their files from disk (composer discard). */
    data object DraftAttachmentsCleared : ChatIntent
    /** Clears draft attachments after send; sent messages keep durable file paths. */
    data object DraftAttachmentsCommitted : ChatIntent
    data object NewConversation : ChatIntent
    data class ConversationOpened(
        val conversation: SidePanelConversation,
        val loadMessages: Boolean = true
    ) : ChatIntent
    data class MessagesLoaded(val conversationId: UUID, val messages: List<SidePanelMessage>) : ChatIntent
    data class UserMessageAppended(val message: SidePanelMessage) : ChatIntent
    data class ActiveConversationRenamed(val id: UUID, val title: String) : ChatIntent
    data class ActiveConversationDeleted(val id: UUID) : ChatIntent
    data object StreamingTurnStarted : ChatIntent
    data class StreamingMerged(
        val result: ChatStreamingMergeResult,
        val bumpStreamingRevision: Boolean = false
    ) : ChatIntent
    data object StreamingErrorDismissed : ChatIntent
    data class SendPreparationFailed(val message: String) : ChatIntent
}

internal fun ChatState.withoutIncompleteAssistantRows(): ChatState =
    copy(messages = messages.filter { it.isComplete || it.role != ChatMessageRole.ASSISTANT })

internal fun ChatState.clearedStreamingFields(): ChatState = copy(
    currentPartialText = "",
    currentPartialThinking = "",
    streamingThinkingId = null,
    streamingAnswerId = null,
    streamingOutputStreamId = null,
    streamErrorMessage = null,
    streamingStatus = ChatStreamingStatus.Idle,
    isSending = false,
    streamingRevision = 0
)
