package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

internal object ChatCompletionsCodec {
    fun encodeRequest(
        modelId: String,
        messages: List<SidePanelMessage>,
        reasoning: SidePanelReasoningModel,
        stream: Boolean = false,
        providerSortBy: String? = null
    ): String = buildString {
        val wireMessages = messages.filter { message ->
            message.kind != SidePanelMessageKind.THINKING &&
                (message.isComplete || message.role != ChatMessageRole.ASSISTANT)
        }
        append("""{"model":""")
        appendQuoted(modelId)
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
        reasoning.effort?.let { effort ->
            append(""","reasoning":{"effort":""")
            appendQuoted(effort)
            append('}')
        }
        if (stream) {
            append(""","stream":true""")
        }
        if (!providerSortBy.isNullOrBlank()) {
            append(""","provider":{"sort":{"by":""")
            appendQuoted(providerSortBy)
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
