package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentError
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ProviderWireTypesMultimodalTest {
    @Test(expected = ChatAttachmentError.VisualEncodingFailed::class)
    fun encodeChatCompletionBody_throwsWhenVisualAttachmentCannotBeEncoded() {
        val attachment = ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.IMAGE,
            filename = "missing.jpg",
            localPath = "/tmp/missing-${UUID.randomUUID()}.jpg",
        )
        val detailJson = ChatTextMessageDetailCodec.encode(
            attachments = listOf(attachment),
            modelContent = "describe",
        )
        val message = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = "describe",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            detailJson = detailJson,
        )

        ProviderWireTypes.encodeChatCompletionBody(
            request = ProviderChatRequest(
                providerId = "openrouter",
                modelId = "gpt-4o",
                messages = listOf(message),
            ),
            reasoningWireStyle = ProviderReasoningWireStyle.TOP_LEVEL_EFFORT,
            supportsProviderRouting = false,
        )
    }

    @Test
    fun encodeChatCompletionBody_usesTextOnlyWhenNoVisualMedia() {
        val body = ProviderWireTypes.encodeChatCompletionBody(
            request = ProviderChatRequest(
                providerId = "openrouter",
                modelId = "gpt-4o",
                messages = listOf(
                    SidePanelMessage(
                        id = UUID.randomUUID(),
                        role = ChatMessageRole.USER,
                        content = "plain text only",
                        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                    ),
                ),
            ),
            reasoningWireStyle = ProviderReasoningWireStyle.TOP_LEVEL_EFFORT,
            supportsProviderRouting = false,
        )

        assertTrue(body.contains("\"content\":\"plain text only\""))
    }
}
