package io.github.bengidev.opencore.chat.utilities

internal object ChatRichContentSegmenter {
    fun segment(markdown: String): List<ChatRichContentSegment> {
        if (markdown.isEmpty()) return emptyList()
        if (ChatStreamingMarkdownGuard.shouldUsePlainFallback(markdown)) {
            return listOf(ChatRichContentSegment.Prose(markdown))
        }
        val parts = markdown.split("```")
        if (parts.size == 1) return listOf(ChatRichContentSegment.Prose(markdown))

        val segments = mutableListOf<ChatRichContentSegment>()
        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) {
                if (part.isNotBlank()) segments += ChatRichContentSegment.Prose(part)
            } else {
                val newline = part.indexOf('\n')
                val language = if (newline < 0) part.trim().lowercase() else part.substring(0, newline).trim().lowercase()
                val body = if (newline < 0) "" else part.substring(newline + 1)
                when (language) {
                    "mermaid" -> segments += ChatRichContentSegment.MermaidDiagram(body.trimEnd('\n'))
                    "latex", "math", "katex" -> segments += ChatRichContentSegment.MathBlock(body.trimEnd('\n'))
                    else -> segments += ChatRichContentSegment.Prose("```$part```")
                }
            }
        }
        return segments.ifEmpty { listOf(ChatRichContentSegment.Prose(markdown)) }
    }
}
