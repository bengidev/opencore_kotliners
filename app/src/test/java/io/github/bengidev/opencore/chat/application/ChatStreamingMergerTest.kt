package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.chat.utilities.ChatAssistantContentNormalizer
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
        assertEquals(SidePanelMessageKind.THINKING, thinking.kind)
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
        val thinkingRows = result.state.messages.filter { it.kind == SidePanelMessageKind.THINKING }
        assertEquals(1, thinkingRows.size)
        assertEquals("A B (note)", thinkingRows.first().content)
    }

    @Test
    fun textDelta_keepsRawContentUntilDone() {
        val raw = "[{'type': 'text', 'text': \"GeForce is NVIDIA's brand for consumer GPUs.\"}]"
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val deltaResult = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta(raw),
            { answerId },
            now
        )
        assertEquals(raw, deltaResult.state.messages.last().content)

        val doneResult = ChatStreamingMerger.merge(
            deltaResult.state,
            ChatStreamingEvent.Done,
            { answerId },
            now
        )
        assertEquals(
            "GeForce is NVIDIA's brand for consumer GPUs.",
            doneResult.state.messages.last().content
        )
    }

    @Test
    fun textDelta_keepsSafetyLabelsUntilDone() {
        val raw = "User Safety: safe\nResponse Safety: safe"
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val deltaResult = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta(raw),
            { answerId },
            now
        )
        assertEquals(raw, deltaResult.state.messages.last().content)

        val doneResult = ChatStreamingMerger.merge(
            deltaResult.state,
            ChatStreamingEvent.Done,
            { answerId },
            now
        )
        assertEquals(
            ChatAssistantContentNormalizer.SAFETY_ONLY_FALLBACK,
            doneResult.state.messages.last().content
        )
    }

    @Test
    fun textDelta_doesNotTreatPartialSafetyLineAsFallback() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val partial = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta("User Safety: safe\n"),
            { answerId },
            now
        ).state
        val withAnswer = ChatStreamingMerger.merge(
            partial,
            ChatStreamingEvent.TextDelta("A computer is a programmable machine."),
            { answerId },
            now
        )
        assertEquals(
            "User Safety: safe\nA computer is a programmable machine.",
            withAnswer.state.messages.last().content
        )

        val doneResult = ChatStreamingMerger.merge(
            withAnswer.state,
            ChatStreamingEvent.Done,
            { answerId },
            now
        )
        assertEquals(
            "User Safety: safe\nA computer is a programmable machine.",
            doneResult.state.messages.last().content
        )
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
        assertEquals(SidePanelMessageKind.TEXT, assistant.kind)
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
    fun applyPendingPartial_updatesThinkingAndAnswerRowsFromAccumulatedContent() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val result = ChatStreamingMerger.applyPendingPartial(
            state = initial,
            partialThinking = "Weighing options",
            partialText = "Hello",
            partialOutputStreamDelta = "",
            makeId = ::makeId,
            now = now
        )
        assertEquals(3, result.state.messages.size)
        assertEquals("Weighing options", result.state.messages[1].content)
        assertEquals("Hello", result.state.messages[2].content)
        assertEquals("Weighing options", result.state.currentPartialThinking)
        assertEquals("Hello", result.state.currentPartialText)
        assertEquals(ChatStreamingStatus.Running, result.state.streamingStatus)
    }

    @Test
    fun applyPendingPartial_reusesOrphanIncompleteRowsWhenStreamingIdsWereCleared() {
        val orphanThinkingId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val orphanAnswerId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val initial = ChatStreamingState(
            messages = listOf(
                userMessage(),
                SidePanelMessage(
                    id = orphanThinkingId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Old think",
                    createdAt = now,
                    kind = SidePanelMessageKind.THINKING,
                    isComplete = false
                ),
                SidePanelMessage(
                    id = orphanAnswerId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Old answer",
                    createdAt = now,
                    kind = SidePanelMessageKind.TEXT,
                    isComplete = false
                )
            )
        )
        val result = ChatStreamingMerger.applyPendingPartial(
            state = initial,
            partialThinking = "New think",
            partialText = "New answer",
            partialOutputStreamDelta = "",
            makeId = ::makeId,
            now = now
        )
        assertEquals(3, result.state.messages.size)
        assertEquals(orphanThinkingId, result.state.streamingThinkingId)
        assertEquals(orphanAnswerId, result.state.streamingAnswerId)
        assertEquals("New think", result.state.messages[1].content)
        assertEquals("New answer", result.state.messages[2].content)
    }

    @Test
    fun applyPendingPartial_isNoOpWhenBuffersAreEmpty() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val result = ChatStreamingMerger.applyPendingPartial(
            state = initial,
            partialThinking = "",
            partialText = "",
            partialOutputStreamDelta = "",
            makeId = ::makeId,
            now = now
        )
        assertEquals(initial, result.state)
    }

    @Test
    fun error_setsFailedStatusAndRemovesIncompleteRows() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(state, ChatStreamingEvent.TextDelta("Partial"), ::makeId, now).state
        val result = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.Error(ChatStreamError("Network down")),
            ::makeId,
            now
        )
        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals("Network down", result.state.streamErrorMessage)
        assertNull(result.state.streamingAnswerId)
        assertEquals(1, result.state.messages.size)
        assertEquals(ChatMessageRole.USER, result.state.messages.first().role)
    }

    @Test
    fun doneAfterError_keepsFailedStatusForErrorBanner() {
        val errored = ChatStreamingMerger.merge(
            ChatStreamingState(messages = listOf(userMessage())),
            ChatStreamingEvent.Error(ChatStreamError("Request timed out. Check your connection and try again.")),
            ::makeId,
            now,
        ).state

        val result = ChatStreamingMerger.merge(errored, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals(
            "Request timed out. Check your connection and try again.",
            result.state.streamErrorMessage,
        )
    }

    @Test
    fun error_removesOrphanIncompleteRowsWhenStreamingIdsAreNull() {
        val orphanAnswerId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val initial = ChatStreamingState(
            messages = listOf(
                userMessage(),
                SidePanelMessage(
                    id = orphanAnswerId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Stale partial",
                    createdAt = now,
                    kind = SidePanelMessageKind.TEXT,
                    isComplete = false
                )
            )
        )
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.Error(ChatStreamError("Missing API key")),
            ::makeId,
            now
        )
        assertEquals(1, result.state.messages.size)
        assertEquals(ChatMessageRole.USER, result.state.messages.first().role)
    }

    @Test
    fun textDelta_createsNewRowWhenPriorTurnHasIncompleteAssistantRow() {
        val staleAnswerId = UUID.fromString("00000000-0000-0000-0000-000000000020")
        val initial = ChatStreamingState(
            messages = listOf(
                userMessage("First"),
                SidePanelMessage(
                    id = staleAnswerId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Old partial",
                    createdAt = now,
                    kind = SidePanelMessageKind.TEXT,
                    isComplete = false
                ),
                userMessage("Second"),
            )
        )
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta("Fresh"),
            { answerId },
            now
        )
        assertEquals(4, result.state.messages.size)
        val assistantRows = result.state.messages.filter {
            it.role == ChatMessageRole.ASSISTANT && it.kind == SidePanelMessageKind.TEXT
        }
        assertEquals(2, assistantRows.size)
        assertEquals("Old partial", assistantRows.first().content)
        assertEquals("Fresh", assistantRows.last().content)
        assertEquals(answerId, result.state.streamingAnswerId)
    }

    @Test
    fun textDelta_createsNewRowWhenTrackedIdPointsAtMissingRow() {
        val missingId = UUID.fromString("00000000-0000-0000-0000-000000000099")
        val initial = ChatStreamingState(
            messages = listOf(userMessage()),
            streamingAnswerId = missingId,
        )
        val result = ChatStreamingMerger.merge(
            initial,
            ChatStreamingEvent.TextDelta("Hello"),
            { answerId },
            now
        )
        assertEquals(2, result.state.messages.size)
        assertEquals("Hello", result.state.messages.last().content)
        assertEquals(answerId, result.state.streamingAnswerId)
    }

    @Test
    fun doneWithoutAssistantRows_marksTurnFailed() {
        val initial = ChatStreamingState(messages = listOf(userMessage()))
        val result = ChatStreamingMerger.merge(initial, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals(ChatStreamingMerger.EMPTY_RESPONSE_MESSAGE, result.state.streamErrorMessage)
        assertEquals(1, result.state.messages.size)
        assertEquals(ChatMessageRole.USER, result.state.messages.first().role)
    }

    @Test
    fun doneWithThinkingOnly_marksTurnFailedAndStripsThinkingRow() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.ThinkingDelta("Consider options"),
            ::makeId,
            now,
        ).state
        val result = ChatStreamingMerger.merge(state, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals(ChatStreamingMerger.EMPTY_RESPONSE_MESSAGE, result.state.streamErrorMessage)
        assertEquals(1, result.state.messages.size)
        assertEquals(ChatMessageRole.USER, result.state.messages.first().role)
    }

    @Test
    fun doneWithWhitespaceOnlyAnswer_marksTurnFailedAndStripsAnswerRow() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(state, ChatStreamingEvent.TextDelta("   "), ::makeId, now).state
        val result = ChatStreamingMerger.merge(state, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
        assertEquals(ChatStreamingMerger.EMPTY_RESPONSE_MESSAGE, result.state.streamErrorMessage)
        assertEquals(1, result.state.messages.size)
        assertEquals(ChatMessageRole.USER, result.state.messages.first().role)
    }

    @Test
    fun done_finalizesResolvedRowsWhenStreamingIdsAreNull() {
        val orphanThinkingId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val orphanAnswerId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val initial = ChatStreamingState(
            messages = listOf(
                userMessage(),
                SidePanelMessage(
                    id = orphanThinkingId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Think",
                    createdAt = now,
                    kind = SidePanelMessageKind.THINKING,
                    isComplete = false
                ),
                SidePanelMessage(
                    id = orphanAnswerId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "Reply",
                    createdAt = now,
                    kind = SidePanelMessageKind.TEXT,
                    isComplete = false
                )
            )
        )
        val result = ChatStreamingMerger.merge(initial, ChatStreamingEvent.Done, ::makeId, now)
        assertTrue(result.state.messages.all { it.isComplete })
        assertEquals(2, result.finalizedMessages.size)
    }

    @Test
    fun outputStreamDeltas_mergeIntoOneRow() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp/project"),
            ::makeId,
            now,
        ).state
        state = ChatStreamingMerger.applyPendingPartial(
            state = state,
            partialThinking = "",
            partialText = "",
            partialOutputStreamDelta = "PASS suite\n",
            makeId = ::makeId,
            now = now,
        ).state
        val result = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamEnded(
                status = ChatOutputStreamStatus.COMPLETED,
                exitCode = 0,
                durationMs = 1200,
            ),
            ::makeId,
            now,
        )

        val rows = result.state.messages.filter { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        assertEquals(1, rows.size)
        val detail = ChatOutputStreamDetailCodec.decode(rows.first().detailJson, rows.first().isComplete)
        assertEquals("npm test", rows.first().content)
        assertEquals("/tmp/project", detail.cwd)
        assertEquals("PASS suite\n", detail.outputTail)
        assertEquals(ChatOutputStreamStatus.COMPLETED, detail.status)
        assertEquals(0, detail.exitCode)
        assertEquals(1200, detail.durationMs)
        assertTrue(rows.first().isComplete)
        assertEquals(1, result.finalizedMessages.size)
    }

    @Test
    fun errorMidStream_finalizesOutputStreamAsFailed() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp/project"),
            ::makeId,
            now,
        ).state
        state = ChatStreamingMerger.applyPendingPartial(
            state = state,
            partialThinking = "",
            partialText = "",
            partialOutputStreamDelta = "partial\n",
            makeId = ::makeId,
            now = now,
        ).state
        val result = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.Error(ChatStreamError("Connection lost.")),
            ::makeId,
            now,
        )

        val row = result.state.messages.single { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        val detail = ChatOutputStreamDetailCodec.decode(row.detailJson, row.isComplete)
        assertEquals("partial\n", detail.outputTail)
        assertEquals(ChatOutputStreamStatus.FAILED, detail.status)
        assertTrue(row.isComplete)
        assertEquals(ChatStreamingStatus.Failed, result.state.streamingStatus)
    }

    @Test
    fun outputStreamDelta_appliesAfterBegin() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp"),
            ::makeId,
            now,
        ).state
        state = ChatStreamingMerger.applyPendingPartial(
            state = state,
            partialThinking = "",
            partialText = "",
            partialOutputStreamDelta = "early output\n",
            makeId = ::makeId,
            now = now,
        ).state
        assertEquals("early output\n", outputStreamDetail(state).outputTail)
    }

    @Test
    fun beginOutputStream_autoFinalizesPreviousStreamAsFailed() {
        val firstStreamId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "first", cwd = null),
            { firstStreamId },
            now,
        ).state
        val result = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "second", cwd = null),
            ::makeId,
            now,
        )

        val streams = result.state.messages.filter { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        assertEquals(2, streams.size)
        val firstDetail = ChatOutputStreamDetailCodec.decode(streams[0].detailJson, streams[0].isComplete)
        assertEquals(ChatOutputStreamStatus.FAILED, firstDetail.status)
        assertTrue(streams[0].isComplete)
        assertFalse(streams[1].isComplete)
        assertEquals(1, result.finalizedMessages.size)
    }

    @Test
    fun done_finalizesThinkingAnswerAndOutputStream() {
        var state = ChatStreamingState(messages = listOf(userMessage()))
        state = ChatStreamingMerger.merge(state, ChatStreamingEvent.ThinkingDelta("Think"), ::makeId, now).state
        state = ChatStreamingMerger.merge(
            state,
            ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp"),
            ::makeId,
            now,
        ).state
        state = ChatStreamingMerger.applyPendingPartial(
            state = state,
            partialThinking = "Think",
            partialText = "Answer",
            partialOutputStreamDelta = "PASS\n",
            makeId = ::makeId,
            now = now,
        ).state
        val result = ChatStreamingMerger.merge(state, ChatStreamingEvent.Done, ::makeId, now)

        assertEquals(ChatStreamingStatus.Done, result.state.streamingStatus)
        assertEquals(3, result.finalizedMessages.size)
        val stream = result.state.messages.single { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        assertEquals("PASS\n", outputStreamDetail(result.state).outputTail)
        assertTrue(stream.isComplete)
        assertTrue(result.state.messages.all { it.isComplete })
    }

    private fun outputStreamDetail(state: ChatStreamingState): ChatOutputStreamDetail {
        val row = state.messages.single { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        return ChatOutputStreamDetailCodec.decode(row.detailJson, row.isComplete)
    }
}
