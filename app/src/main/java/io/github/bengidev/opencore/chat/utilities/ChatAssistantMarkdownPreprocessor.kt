package io.github.bengidev.opencore.chat.utilities

/** Normalizes assistant markdown so streamed prose breaks into readable sections. */
internal object ChatAssistantMarkdownPreprocessor {
    fun normalize(markdown: String): String {
        if (markdown.isEmpty()) return markdown

        val segments = markdown.split("```")
        val normalized = segments.mapIndexed { index, segment ->
            if (index % 2 == 0) normalizeProseSegment(segment) else segment
        }
        return normalized.joinToString("```")
    }

    private fun normalizeProseSegment(segment: String): String {
        var text = segment
        text = text.replace(Regex("(?<=[.!?])(\\*\\*[^*\\n]{2,80}:\\*\\*)"), "\n\n$1")
        text = text.replace(Regex("(?<=[^\\n])(\\*\\*[^*\\n]{2,80}:\\*\\*)"), "\n\n$1")
        text = text.replace(Regex("(?<=[^\\n])(#{1,6}\\s+\\S[^\\n]*)"), "\n\n$1")
        text = text.replace(Regex("(?<=[^\\n])(——+\\s*\\*\\*)"), "\n\n$1")
        text = text.replace(Regex("(?<=[.!?])\\s+([-•*]\\s+)"), "\n\n$1")
        text = text.replace(Regex("(?<=\\S)\\s+([-•*]\\s+\\S)"), "\n\n$1")
        text = text.replace(Regex("(?<=\\S)\\s+(\\d+\\.\\s+\\S)"), "\n\n$1")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text
            .lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\\n\\n\\n+"), "\n\n")
    }
}
