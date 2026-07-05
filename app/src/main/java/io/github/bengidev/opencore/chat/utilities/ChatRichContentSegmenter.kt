package io.github.bengidev.opencore.chat.utilities

internal object ChatRichContentSegmenter {
    fun segment(markdown: String, progressive: Boolean = false): List<ChatRichContentSegment> {
        if (markdown.isEmpty()) return emptyList()
        if (!progressive && ChatStreamingMarkdownGuard.shouldUsePlainFallback(markdown)) {
            return listOf(ChatRichContentSegment.Prose(markdown))
        }

        val parts = markdown.split("```")
        if (parts.size == 1) {
            return listOf(classifyProse(parts.single(), progressive))
        }

        val segments = mutableListOf<ChatRichContentSegment>()
        val hasUnclosedFence = parts.size % 2 == 0
        val lastClosedPartIndex = if (progressive && hasUnclosedFence) parts.lastIndex - 1 else parts.lastIndex

        for (index in 0..lastClosedPartIndex) {
            val part = parts[index]
            if (index % 2 == 0) {
                if (part.isNotBlank()) segments += classifyProse(part, progressive)
            } else {
                segments += classifyFence(part)
            }
        }

        if (progressive && hasUnclosedFence) {
            segments += ChatRichContentSegment.RawFragment("```${parts.last()}")
        }

        return segments.ifEmpty { listOf(ChatRichContentSegment.Prose(markdown)) }
    }

    private fun classifyProse(text: String, progressive: Boolean): ChatRichContentSegment =
        if (progressive && ChatStreamingMarkdownGuard.shouldUsePlainFallback(text)) {
            ChatRichContentSegment.RawFragment(text)
        } else {
            ChatRichContentSegment.Prose(text)
        }

    private fun classifyFence(part: String): ChatRichContentSegment {
        val newline = part.indexOf('\n')
        val language = if (newline < 0) part.trim().lowercase() else part.substring(0, newline).trim().lowercase()
        val body = if (newline < 0) "" else part.substring(newline + 1)
        return when (language) {
            "mermaid" -> ChatRichContentSegment.MermaidDiagram(body.trimEnd('\n'))
            "latex", "math", "katex" -> ChatRichContentSegment.MathBlock(body.trimEnd('\n'))
            else -> ChatRichContentSegment.Prose("```$part```")
        }
    }
}
