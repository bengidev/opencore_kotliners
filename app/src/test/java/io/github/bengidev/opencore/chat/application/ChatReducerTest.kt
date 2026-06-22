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
    fun conversationOpened_setsActiveConversation() {
        val target = conversation("Resume me")
        val result = ChatReducer.reduce(
            ChatState(),
            ChatIntent.ConversationOpened(target)
        )
        assertEquals(target, result.activeConversation)
        assertTrue(result.isThreadActive)
        assertEquals("Resume me", result.headerTitle)
    }

    @Test
    fun messagesLoaded_replacesMessageList() {
        val loaded = listOf(
            message(ChatMessageRole.USER, "One"),
            message(ChatMessageRole.ASSISTANT, "Two")
        )
        val result = ChatReducer.reduce(
            ChatState(activeConversation = conversation()),
            ChatIntent.MessagesLoaded(loaded)
        )
        assertEquals(loaded, result.messages)
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
    fun sendStartedAndFinished_toggleSendingFlag() {
        val sending = ChatReducer.reduce(ChatState(), ChatIntent.SendStarted)
        assertTrue(sending.isSending)
        val finished = ChatReducer.reduce(sending, ChatIntent.SendFinished)
        assertFalse(finished.isSending)
    }
}
