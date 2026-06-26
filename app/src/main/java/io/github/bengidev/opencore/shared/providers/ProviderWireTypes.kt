package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

internal object ProviderWireTypes {
    fun encodeChatCompletionBody(
        request: ProviderChatRequest,
        reasoningWireStyle: ProviderReasoningWireStyle,
        supportsProviderRouting: Boolean,
        stream: Boolean = true
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
            appendQuoted(message.content)
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

    fun parseErrorMessage(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return ChatJsonStringField.extract(responseBody, "message")?.takeIf { it.isNotBlank() }
    }

    private fun StringBuilder.appendQuoted(value: String) {
        ChatJsonStringField.appendQuoted(this, value)
    }
}
