package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ChatCompletionsCodecTest {

    @Test
    fun encodeRequest_includesModelMessagesAndReasoning() {
        val body = ChatCompletionsCodec.encodeRequest(
            modelId = "openrouter/free",
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "hello",
                    createdAt = Instant.now()
                )
            ),
            reasoningEffort = "high"
        )

        assertTrue(body.contains(""""model":"openrouter/free""""))
        assertTrue(body.contains(""""role":"user""""))
        assertTrue(body.contains(""""content":"hello""""))
        assertTrue(body.contains(""""effort":"high""""))
    }

    @Test
    fun encodeRequest_omitsIncompleteAssistantRows() {
        val body = ChatCompletionsCodec.encodeRequest(
            modelId = "openrouter/free",
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "hello",
                    createdAt = Instant.now()
                ),
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.ASSISTANT,
                    content = "partial",
                    createdAt = Instant.now(),
                    isComplete = false
                ),
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.ASSISTANT,
                    content = "thinking",
                    createdAt = Instant.now(),
                    kind = SidePanelMessageKind.THINKING,
                    isComplete = false
                )
            )
        )

        assertTrue(body.contains(""""content":"hello""""))
        assertFalse(body.contains("partial"))
        assertFalse(body.contains("thinking"))
    }

    @Test
    fun encodeRequest_withStream_includesStreamFlag() {
        val body = ChatCompletionsCodec.encodeRequest(
            modelId = "openrouter/free",
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "hello",
                    createdAt = Instant.now()
                )
            ),
            stream = true
        )

        assertTrue(body.contains(""""stream":true"""))
    }

    @Test
    fun encodeRequest_fastMode_includesProviderRouting() {
        val body = ChatCompletionsCodec.encodeRequest(
            modelId = "openrouter/free",
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "Hi",
                    createdAt = Instant.now()
                )
            ),
            stream = true,
            providerSortBy = "throughput"
        )

        assertTrue(body.contains(""""provider":{"sort":{"by":"throughput","partition":"none"}}"""))
    }

    @Test
    fun encodeRequest_standardMode_omitsProviderRouting() {
        val body = ChatCompletionsCodec.encodeRequest(
            modelId = "openrouter/free",
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "Hi",
                    createdAt = Instant.now()
                )
            ),
            stream = true
        )

        assertFalse(body.contains(""""provider""""))
    }

    @Test
    fun parseErrorMessage_readsNestedError() {
        val message = ChatCompletionsCodec.parseErrorMessage(
            """{"error":{"message":"Invalid API key"}}"""
        )

        assertEquals("Invalid API key", message)
    }
}
