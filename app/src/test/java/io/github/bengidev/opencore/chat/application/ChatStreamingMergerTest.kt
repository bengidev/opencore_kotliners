package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageKind
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ChatStreamingMergerTest {

    private val now = Instant.parse("2024-01-01T00:00:00Z")
    private val thinkingId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val answerId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private var nextId = 0

    private fun makeId(): UUID = listOf(thinkingId, answerId)[nextId++ % 2]

    private fun userMessage(content: String = "Hello") = SidePanelMessage(
        id = UUID.randomUUID(),
        role = ChatMessageRole.USER,
        content = content,
        createdAt = now
    )

    @Test
    fun thinkingDelta_createsSingleThinkingRow() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.ThinkingDelta("Weighing "),
            { thinkingId },
            now
        )
        assertEquals(2, result.state.messages.size)
        val thinking = result.state.messages.last()
        assertEquals(ChatMessageKind.THINKING, thinking.kind)
        assertEquals("Weighing ", thinking.content)
        assertFalse(thinking.isComplete)
        assertEquals(thinkingId, result.state.streamingThinkingId)
        assertEquals(ChatStreamingStatus.Running, result.state.streamingStatus)
    }

    @Test
    fun lateThinkingDelta_mergesIntoExistingRow() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.ThinkingDelta("A "),
            ::makeId,
            now
        ).state
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.ThinkingDelta("B "),
            ::makeId,
            now
        ).state
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.TextDelta("Answer"),
            ::makeId,
            now
        ).state
        val result = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.ThinkingDelta("(note)"),
            ::makeId,
            now
        )
        val thinkingRows = result.state.messages.filter { it.kind == ChatMessageKind.THINKING }
        assertEquals(1, thinkingRows.size)
        assertEquals("A B (note)", thinkingRows.first().content)
    }

    @Test
    fun textDelta_createsAssistantRow() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta("Hello"),
            { answerId },
            now
        )
        val assistant = result.state.messages.last()
        assertEquals(ChatMessageKind.TEXT, assistant.kind)
        assertEquals(ChatMessageRole.ASSISTANT, assistant.role)
        assertEquals("Hello", assistant.content)
        assertFalse(assistant.isComplete)
        assertEquals(answerId, result.state.streamingAnswerId)
    }

    @Test
    fun done_finalizesRowsAndClearsPartialState() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(state, ChatStreamingEvent.ThinkingDelta("Think"), ::makeId, now).state
        state = ChatStreamingMerger.merge(state, ChatStreamingEvent.TextDelta("Reply"), ::makeId, now).state
        val result = ChatStreamingMerger.merge(state, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Done, result.state.streamingStatus)
        assertNull(result.state.streamingThinkingId)
        assertNull(result.state.streamingAnswerId)
        assertEquals("", result.state.currentPartialText)
        assertEquals(2, result.finalizedMessages.size)
        assertTrue(result.finalizedMessages.all { it.isComplete })
    }

    @Test
    fun error_setsFailedStatusAndClearsStreamingIds() {
        val initial = ChatStreamingState(
            messages = listOf(userMessage()),
            streamingThinkingId = thinkingId,
            streamingStatus = ChatStreamingStatus.Running
        )
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.Error(ChatStreamError("Network down")),
            ::makeId,
            now
        )
        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals("Network down", result.state.streamErrorMessage)
        assertNull(result.state.streamingThinkingId)
    }
}
