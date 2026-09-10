package io.github.bengidev.opencore.chat.utilities

internal object ChatRichContentSegmenter {
    fun segment(markdown: String, progressive: Boolean = false): List<ChatRichContentSegment> {
        if (markdown.isEmpty()) return emptyList()
        if (!progressive && ChatStreamingMarkdownGuard.shouldUsePlainFallback(markdown)) {
            return listOf(ChatRichContentSegment.Prose(markdown))
        }

        val parts = markdown.split("```")
        if (parts.size == 1) {
            return classifyProseSegments(parts.single(), progressive)
        }

        val segments = mutableListOf<ChatRichContentSegment>()
        val hasUnclosedFence = parts.size % 2 == 0
        val lastClosedPartIndex = if (progressive && hasUnclosedFence) parts.lastIndex - 1 else parts.lastIndex

        for (index in 0..lastClosedPartIndex) {
            val part = parts[index]
            if (index % 2 == 0) {
                segments += classifyProseSegments(part, progressive)
            } else {
                segments += classifyFence(part)
            }
        }

        if (progressive && hasUnclosedFence) {
            segments += segmentsFromProgressiveTail("```${parts.last()}")
        }

        return segments.ifEmpty { listOf(ChatRichContentSegment.Prose(markdown)) }
    }

    private fun classifyProseSegments(text: String, progressive: Boolean): List<ChatRichContentSegment> {
        if (text.isEmpty()) return emptyList()
        if (!progressive) {
            return listOf(
                if (ChatStreamingMarkdownGuard.shouldUsePlainFallback(text)) {
                    ChatRichContentSegment.Prose(text)
                } else {
                    ChatRichContentSegment.Prose(text)
                }
            )
        }

        if (!ChatStreamingMarkdownGuard.shouldUsePlainFallback(text)) {
            return listOf(ChatRichContentSegment.Prose(text))
        }

        val delimiterIndex = earliestIncompleteDelimiterIndex(text)
        if (delimiterIndex == null) {
            return listOf(ChatRichContentSegment.Prose(text))
        }

        val output = mutableListOf<ChatRichContentSegment>()
        val prefix = text.substring(0, delimiterIndex)
        val tail = text.substring(delimiterIndex)
        if (prefix.isNotEmpty()) {
            output += classifyProseSegments(prefix, progressive = true)
        }
        if (tail.isNotEmpty()) {
            output += segmentsFromProgressiveTail(tail)
        }
        return output
    }

    /** Pulls complete markdown blocks out of a progressive tail for rich rendering. */
    private fun segmentsFromProgressiveTail(tail: String): List<ChatRichContentSegment> {
        val lines = tail.split('\n')
        val output = mutableListOf<ChatRichContentSegment>()
        val plainLines = mutableListOf<String>()
        var index = 0

        fun flushPlainLines() {
            if (plainLines.isEmpty()) return
            output += ChatRichContentSegment.RawFragment(plainLines.joinToString("\n"))
            plainLines.clear()
        }

        while (index < lines.size) {
            val line = lines[index]
            when {
                isGfmTableHeader(line) &&
                    index + 1 < lines.size &&
                    isGfmTableSeparator(lines[index + 1]) -> {
                    flushPlainLines()
                    val tableLines = mutableListOf(lines[index], lines[index + 1])
                    index += 2
                    while (index < lines.size && isGfmTableRow(lines[index])) {
                        tableLines += lines[index]
                        index++
                    }
                    output += ChatRichContentSegment.Prose("\n\n${tableLines.joinToString("\n")}")
                }
                isMarkdownHeadingLine(line) || isThematicBreakLine(line) -> {
                    flushPlainLines()
                    output += ChatRichContentSegment.Prose(line)
                    index++
                }
                isMarkdownListLine(line) -> {
                    flushPlainLines()
                    val listLines = mutableListOf(line)
                    index++
                    while (index < lines.size && isMarkdownListLine(lines[index])) {
                        listLines += lines[index]
                        index++
                    }
                    output += ChatRichContentSegment.Prose(listLines.joinToString("\n"))
                }
                isMarkdownBlockquoteLine(line) -> {
                    flushPlainLines()
                    val quoteLines = mutableListOf(line)
                    index++
                    while (index < lines.size && isMarkdownBlockquoteLine(lines[index])) {
                        quoteLines += lines[index]
                        index++
                    }
                    output += ChatRichContentSegment.Prose(quoteLines.joinToString("\n"))
                }
                isGfmTableRow(line) -> {
                    flushPlainLines()
                    output += ChatRichContentSegment.Prose(line)
                    index++
                }
                else -> {
                    plainLines += line
                    index++
                }
            }
        }

        flushPlainLines()
        return output
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

    private fun isGfmTableRow(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length > 2
    }

    private fun isGfmTableHeader(line: String): Boolean = isGfmTableRow(line)

    private fun isGfmTableSeparator(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) return false
        val inner = trimmed.drop(1).dropLast(1)
        if (inner.isEmpty()) return false
        return inner.all { it == '|' || it == '-' || it == ':' || it == ' ' }
    }

    private fun isMarkdownHeadingLine(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("#")) return false
        return Regex("^#{1,6}\\s+\\S").containsMatchIn(trimmed)
    }

    private fun isMarkdownListLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        if (Regex("^[-*•]\\s+\\S").containsMatchIn(trimmed)) return true
        return Regex("^\\d+\\.\\s+\\S").containsMatchIn(trimmed)
    }

    private fun isMarkdownBlockquoteLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith(">") && trimmed.length > 1
    }

    private fun isThematicBreakLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.all { it == '-' || it == ' ' || it == '*' || it == '_' } && trimmed.contains('-')
    }

    private fun earliestIncompleteDelimiterIndex(text: String): Int? {
        val candidates = listOfNotNull(
            incompleteBacktickIndex(text),
        )
        return candidates.minOrNull()
    }

    private fun incompleteBacktickIndex(text: String): Int? {
        var inFence = false
        var inlineBackticks = 0
        var index = 0
        while (index < text.length) {
            if (text.startsWith("```", index)) {
                inFence = !inFence
                inlineBackticks = 0
                index += 3
                continue
            }
            if (!inFence && text[index] == '`') {
                inlineBackticks++
            }
            index++
        }
        if (!inFence && inlineBackticks % 2 != 0) {
            var position = text.length - 1
            while (position >= 0) {
                if (text[position] == '`') return position
                position--
            }
        }
        return null
    }
}
