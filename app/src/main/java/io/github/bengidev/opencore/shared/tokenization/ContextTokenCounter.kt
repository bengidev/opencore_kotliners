package io.github.bengidev.opencore.shared.tokenization

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingType
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.infrastructure.attachments
import io.github.bengidev.opencore.chat.infrastructure.providerContent
import io.github.bengidev.opencore.chat.utilities.ChatAssistantContentNormalizer
import io.github.bengidev.opencore.chat.utilities.ChatMultimodalWireLogic
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import java.util.concurrent.atomic.AtomicReference

/** Tiktoken-backed token counting with a character heuristic fallback. */
internal object ContextTokenCounter {
    private val encodingRef = AtomicReference<com.knuddels.jtokkit.api.Encoding?>(null)

    fun warmUp() {
        if (encodingRef.get() != null) return
        val registry = Encodings.newDefaultEncodingRegistry()
        encodingRef.set(registry.getEncoding(EncodingType.CL100K_BASE))
    }

    fun installEncoderForTesting(encoder: com.knuddels.jtokkit.api.Encoding?) {
        encodingRef.set(encoder)
    }

    fun countTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        val encoder = encodingRef.get()
        return if (encoder != null) {
            encoder.countTokens(trimmed)
        } else {
            heuristicTokenCount(trimmed)
        }
    }

    fun countTokens(message: SidePanelMessage): Int = countTokens(messageText(message))

    fun countTokens(messages: List<SidePanelMessage>, draft: String? = null): Int {
        var total = messages.sumOf { countTokens(it) }
        if (draft != null) {
            total += countTokens(draft)
        }
        return total
    }

    fun heuristicTokenCount(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return (trimmed.length + 3) / 4
    }

    private fun messageText(message: SidePanelMessage): String {
        if (!message.isComplete && message.role == ChatMessageRole.ASSISTANT) return ""
        val base = when (message.kind) {
            SidePanelMessageKind.OUTPUT_STREAM -> {
                val detail = ChatOutputStreamDetailCodec.decode(message.detailJson, message.isComplete)
                message.content + "\n" + detail.outputTail
            }
            else -> ChatAssistantContentNormalizer.displayText(message.providerContent())
        }
        val wireOverhead = ChatMultimodalWireLogic.estimatedWireTokenOverhead(message.attachments())
        return if (wireOverhead > 0) {
            base + " ".repeat(wireOverhead * 4)
        } else {
            base
        }
    }
}
