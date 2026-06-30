package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentStore
import io.github.bengidev.opencore.sidepanel.domain.dedupeByThreadItemKey

internal object ChatReducer {
    fun reduce(state: ChatState, intent: ChatIntent): ChatState = when (intent) {
        is ChatIntent.DraftAttachmentAdded -> state.copy(
            draftAttachments = state.draftAttachments + intent.attachment,
        )
        is ChatIntent.DraftAttachmentRemoved -> {
            state.draftAttachments.firstOrNull { it.id == intent.id }?.let { attachment ->
                ChatAttachmentStore.remove(attachment.localPath)
            }
            state.copy(draftAttachments = state.draftAttachments.filterNot { it.id == intent.id })
        }
        ChatIntent.DraftAttachmentsCleared -> {
            ChatAttachmentStore.removeAll(state.draftAttachments.map { it.localPath })
            state.copy(draftAttachments = emptyList())
        }
        ChatIntent.DraftAttachmentsCommitted ->
            state.copy(draftAttachments = emptyList())
        ChatIntent.NewConversation -> state.clearedStreamingFields().copy(
            activeConversation = null,
            messages = emptyList(),
            isLoadingMessages = false,
        ).alsoClearDraftAttachmentFiles(state.draftAttachments)
        is ChatIntent.ConversationOpened -> if (intent.loadMessages) {
            state.clearedStreamingFields().copy(
                activeConversation = intent.conversation,
                messages = emptyList(),
                isLoadingMessages = true,
            ).alsoClearDraftAttachmentFiles(state.draftAttachments)
        } else {
            state.clearedStreamingFields().copy(
                activeConversation = intent.conversation,
                isLoadingMessages = false,
            ).alsoClearDraftAttachmentFiles(state.draftAttachments)
        }
        is ChatIntent.MessagesLoaded -> {
            if (state.activeConversation?.id != intent.conversationId) {
                state
            } else {
                state.clearedStreamingFields().copy(
                    messages = intent.messages.dedupeByThreadItemKey(),
                    isLoadingMessages = false
                )
            }
        }
        is ChatIntent.UserMessageAppended -> state.copy(
            messages = (state.messages + intent.message).dedupeByThreadItemKey()
        )
        is ChatIntent.ActiveConversationRenamed -> {
            if (state.activeConversation?.id != intent.id) {
                state
            } else {
                val trimmed = intent.title.trim()
                if (trimmed.isEmpty()) state else {
                    state.copy(
                        activeConversation = state.activeConversation.copy(title = trimmed)
                    )
                }
            }
        }
        is ChatIntent.ActiveConversationDeleted -> {
            if (state.activeConversation?.id != intent.id) {
                state
            } else {
                state.clearedStreamingFields().copy(
                    activeConversation = null,
                    messages = emptyList(),
                    isLoadingMessages = false,
                ).alsoClearDraftAttachmentFiles(state.draftAttachments)
            }
        }
        ChatIntent.StreamingTurnStarted -> state
            .withoutIncompleteAssistantRows()
            .copy(
                isSending = true,
                streamingStatus = ChatStreamingStatus.Running,
                currentPartialText = "",
                currentPartialThinking = "",
                streamErrorMessage = null,
                streamingThinkingId = null,
                streamingAnswerId = null,
                streamingOutputStreamId = null,
                streamingRevision = 0
            )
        is ChatIntent.StreamingMerged -> {
            val stillSending = when (intent.result.state.streamingStatus) {
                ChatStreamingStatus.Running -> true
                ChatStreamingStatus.Done, ChatStreamingStatus.Failed -> false
                ChatStreamingStatus.Idle -> state.isSending
            }
            state.applyStreamingMerge(
                intent.result,
                isSending = stillSending,
                bumpStreamingRevision = intent.bumpStreamingRevision
            )
        }
        ChatIntent.StreamingErrorDismissed -> state
            .withoutIncompleteAssistantRows()
            .clearedStreamingFields()
        is ChatIntent.SendPreparationFailed -> state.copy(streamErrorMessage = intent.message)
    }

    private fun ChatState.alsoClearDraftAttachmentFiles(
        attachments: List<ChatMessageAttachment>,
    ): ChatState {
        ChatAttachmentStore.removeAll(attachments.map { it.localPath })
        return copy(draftAttachments = emptyList())
    }
}
