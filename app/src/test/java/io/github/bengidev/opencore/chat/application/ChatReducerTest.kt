package io.github.bengidev.opencore.chat.application

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant
import java.util.UUID

class ChatReducerTest {

    private val conversationId = UUID.randomUUID()

    private fun conversation(title: String = "Kotlin help") = SidePanelConversation(
        id = conversationId,
        title = title,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z")
    )

    private fun message(role: String, content: String) = SidePanelMessage(
        id = UUID.randomUUID(),
        role = role,
        content = content,
        createdAt = Instant.parse("2024-01-01T00:01:00Z")
    )

    @Test
    fun newConversation_clearsActiveThread() {
        val initial = ChatState(
            activeConversation = conversation(),
            messages = listOf(message(ChatMessageRole.USER, "Hi"))
        )
        val result = ChatReducer.reduce(initial, ChatIntent.NewConversation)
        assertNull(result.activeConversation)
        assertTrue(result.messages.isEmpty())
        assertFalse(result.isThreadActive)
    }

    @Test
    fun conversationOpened_clearsMessagesAndSetsLoading() {
        val target = conversation("Resume me")
        val result = ChatReducer.reduce(
            ChatState(
                messages = listOf(message(ChatMessageRole.USER, "Stale"))
            ),
            ChatIntent.ConversationOpened(target)
        )
        assertEquals(target, result.activeConversation)
        assertTrue(result.messages.isEmpty())
        assertTrue(result.isLoadingMessages)
    }

    @Test
    fun conversationOpened_withoutLoad_keepsComposerReady() {
        val target = conversation("New chat")
        val result = ChatReducer.reduce(
            ChatState(),
            ChatIntent.ConversationOpened(target, loadMessages = false)
        )
        assertEquals(target, result.activeConversation)
        assertFalse(result.isLoadingMessages)
    }

    @Test
    fun messagesLoaded_replacesMessageListWhenConversationMatches() {
        val loaded = listOf(
            message(ChatMessageRole.USER, "One"),
            message(ChatMessageRole.ASSISTANT, "Two")
        )
        val result = ChatReducer.reduce(
            ChatState(activeConversation = conversation(), isLoadingMessages = true),
            ChatIntent.MessagesLoaded(conversationId, loaded)
        )
        assertEquals(loaded, result.messages)
        assertFalse(result.isLoadingMessages)
    }

    @Test
    fun messagesLoaded_ignoredWhenConversationIdMismatch() {
        val stale = listOf(message(ChatMessageRole.USER, "Wrong thread"))
        val otherId = UUID.randomUUID()
        val result = ChatReducer.reduce(
            ChatState(
                activeConversation = conversation(),
                messages = listOf(message(ChatMessageRole.USER, "Current"))
            ),
            ChatIntent.MessagesLoaded(otherId, stale)
        )
        assertEquals(1, result.messages.size)
        assertEquals("Current", result.messages.first().content)
    }

    @Test
    fun userMessageAppended_appendsToList() {
        val user = message(ChatMessageRole.USER, "Hello")
        val result = ChatReducer.reduce(
            ChatState(activeConversation = conversation()),
            ChatIntent.UserMessageAppended(user)
        )
        assertEquals(listOf(user), result.messages)
    }

    @Test
    fun activeConversationRenamed_updatesTitleWhenIdsMatch() {
        val result = ChatReducer.reduce(
            ChatState(activeConversation = conversation("Old")),
            ChatIntent.ActiveConversationRenamed(conversationId, "  New title  ")
        )
        assertEquals("New title", result.activeConversation?.title)
    }

    @Test
    fun activeConversationRenamed_isNoOpWhenIdDiffers() {
        val state = ChatState(activeConversation = conversation("Keep"))
        val result = ChatReducer.reduce(
            state,
            ChatIntent.ActiveConversationRenamed(UUID.randomUUID(), "Other")
        )
        assertEquals(state, result)
    }

    @Test
    fun activeConversationDeleted_clearsThreadWhenIdsMatch() {
        val result = ChatReducer.reduce(
            ChatState(
                activeConversation = conversation(),
                messages = listOf(message(ChatMessageRole.USER, "Hi"))
            ),
            ChatIntent.ActiveConversationDeleted(conversationId)
        )
        assertNull(result.activeConversation)
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun streamingTurnStarted_setsSendingFlag() {
        val sending = ChatReducer.reduce(ChatState(), ChatIntent.StreamingTurnStarted)
        assertTrue(sending.isSending)
        assertEquals(0, sending.streamingRevision)
    }

    @Test
    fun streamingTurnStarted_stripsIncompleteAssistantRows() {
        val orphanId = UUID.randomUUID()
        val state = ChatState(
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "Hi",
                    createdAt = java.time.Instant.parse("2024-01-01T00:00:00Z"),
                ),
                SidePanelMessage(
                    id = orphanId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Partial",
                    createdAt = java.time.Instant.parse("2024-01-01T00:00:00Z"),
                    isComplete = false,
                )
            )
        )
        val result = ChatReducer.reduce(state, ChatIntent.StreamingTurnStarted)
        assertEquals(1, result.messages.size)
        assertEquals(ChatMessageRole.USER, result.messages.first().role)
    }

    @Test
    fun streamingMerged_bumpsRevisionWhenRequested() {
        val mergeResult = ChatStreamingMergeResult(
            state = ChatStreamingState(
                messages = emptyList(),
                streamingStatus = io.github.bengidev.opencore.chat.domain.ChatStreamingStatus.Running
            )
        )
        val result = ChatReducer.reduce(
            ChatState(streamingRevision = 2),
            ChatIntent.StreamingMerged(mergeResult, bumpStreamingRevision = true)
        )
        assertEquals(3, result.streamingRevision)
    }

    @Test
    fun newConversation_resetsStreamingRevision() {
        val result = ChatReducer.reduce(
            ChatState(streamingRevision = 5, activeConversation = conversation()),
            ChatIntent.NewConversation
        )
        assertEquals(0, result.streamingRevision)
    }

    @Test
    fun streamingErrorDismissed_clearsErrorAndIncompleteAssistantRows() {
        val partial = message(ChatMessageRole.ASSISTANT, "Partial").copy(isComplete = false)
        val failed = ChatState(
            messages = listOf(
                message(ChatMessageRole.USER, "Hi"),
                partial
            ),
            streamErrorMessage = "Oops"
        )
        val result = ChatReducer.reduce(failed, ChatIntent.StreamingErrorDismissed)
        assertNull(result.streamErrorMessage)
        assertEquals(1, result.messages.size)
        assertEquals(ChatMessageRole.USER, result.messages.first().role)
    }
}
