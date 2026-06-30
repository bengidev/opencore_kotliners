package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField
import io.github.bengidev.opencore.chat.infrastructure.attachments
import io.github.bengidev.opencore.chat.infrastructure.providerContent
import io.github.bengidev.opencore.chat.utilities.ChatMultimodalWireLogic
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

internal object ProviderWireTypes {
    fun encodeChatCompletionBody(
        request: ProviderChatRequest,
        reasoningWireStyle: ProviderReasoningWireStyle,
        supportsProviderRouting: Boolean,
        stream: Boolean = true,
    ): String = buildString {
        val wireMessages = request.messages.filter { message ->
            message.kind != SidePanelMessageKind.THINKING &&
                (message.isComplete || message.role != ChatMessageRole.ASSISTANT)
        }
        append("""{"model":""")
        appendQuoted(request.modelId)
        append(""","messages":[""")
        wireMessages.forEachIndexed { index, message ->
            if (index > 0) append(',')
            append("""{"role":""")
            appendQuoted(message.role)
            append(""","content":""")
            encodeMessageContent(message)
            append('}')
        }
        append(']')
        when (reasoningWireStyle) {
            ProviderReasoningWireStyle.REASONING_OBJECT -> {
                request.reasoningEffort?.let { effort ->
                    append(""","reasoning":{"effort":""")
                    appendQuoted(effort)
                    append("}")
                }
            }
            ProviderReasoningWireStyle.TOP_LEVEL_EFFORT -> {
                request.reasoningEffort?.let { effort ->
                    append(""","reasoning_effort":""")
                    appendQuoted(effort)
                }
            }
        }
        if (stream) {
            append(""","stream":true""")
        }
        val sortBy = if (supportsProviderRouting) request.providerSortBy else null
        if (!sortBy.isNullOrBlank()) {
            append(""","provider":{"sort":{"by":""")
            appendQuoted(sortBy)
            append(""","partition":"none"}}""")
        }
        append('}')
    }

    private fun StringBuilder.encodeMessageContent(message: SidePanelMessage) {
        val modelText = message.providerContent()
        val attachments = message.attachments()
        val parts = runCatching {
            ChatMultimodalWireLogic.makeContentParts(modelText, attachments)
        }.getOrNull()

        if (parts.isNullOrEmpty()) {
            appendQuoted(modelText)
            return
        }

        append('[')
        parts.forEachIndexed { index, part ->
            if (index > 0) append(',')
            append('{')
            append("\"type\":")
            appendQuoted(part.type)
            when (part.type) {
                "text" -> {
                    append(",\"text\":")
                    appendQuoted(part.text.orEmpty())
                }
                "image_url" -> {
                    append(",\"image_url\":{\"url\":")
                    appendQuoted(part.imageUrl.orEmpty())
                    append('}')
                }
                "video_url" -> {
                    append(",\"video_url\":{\"url\":")
                    appendQuoted(part.videoUrl.orEmpty())
                    append('}')
                }
            }
            append('}')
        }
        append(']')
    }

    fun parseErrorMessage(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return ChatJsonStringField.extract(responseBody, "message")?.takeIf { it.isNotBlank() }
    }

    private fun StringBuilder.appendQuoted(value: String) {
        ChatJsonStringField.appendQuoted(this, value)
    }
}
