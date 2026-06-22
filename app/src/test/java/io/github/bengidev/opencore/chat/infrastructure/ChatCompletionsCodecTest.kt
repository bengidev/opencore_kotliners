package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import org.junit.Assert.assertEquals
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
            reasoning = SidePanelReasoningModel.High
        )

        assertTrue(body.contains(""""model":"openrouter/free""""))
        assertTrue(body.contains(""""role":"user""""))
        assertTrue(body.contains(""""content":"hello""""))
        assertTrue(body.contains(""""effort":"high""""))
    }

    @Test
    fun decodeAssistantContent_readsFirstChoice() {
        val content = ChatCompletionsCodec.decodeAssistantContent(
            """
            {
              "choices": [
                { "message": { "role": "assistant", "content": "Hi there" } }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Hi there", content)
    }

    @Test
    fun parseErrorMessage_readsNestedError() {
        val message = ChatCompletionsCodec.parseErrorMessage(
            """{"error":{"message":"Invalid API key"}}"""
        )

        assertEquals("Invalid API key", message)
    }
}
