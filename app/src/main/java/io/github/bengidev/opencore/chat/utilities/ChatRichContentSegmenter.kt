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
            return splitProseWithBlockMath(text, progressive = false)
        }

        val delimiterIndex = earliestIncompleteDelimiterIndex(text)
        if (delimiterIndex != null) {
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

        if (!ChatStreamingMarkdownGuard.shouldUsePlainFallback(text)) {
            return splitProgressiveStablePrefix(text)
        }

        return listOf(ChatRichContentSegment.Prose(text))
    }

    private fun splitProseWithBlockMath(text: String, progressive: Boolean): List<ChatRichContentSegment> {
        if (text.isEmpty()) return emptyList()

        var remaining = text
        val output = mutableListOf<ChatRichContentSegment>()

        while (remaining.isNotEmpty()) {
            val match = firstBlockMathMatch(remaining)
            if (match == null) {
                output += splitMarkdownProse(remaining, progressive)
                break
            }
            val prefix = remaining.substring(0, match.range.first)
            if (prefix.isNotEmpty()) {
                output += splitMarkdownProse(prefix, progressive)
            }
            output += ChatRichContentSegment.MathBlock(match.latex)
            remaining = remaining.substring(match.range.last + 1)
        }

        return output
    }

    private fun splitMarkdownProse(text: String, progressive: Boolean): List<ChatRichContentSegment> {
        if (text.isEmpty()) return emptyList()
        if (!progressive) {
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
            output += splitMarkdownProse(prefix, progressive = true)
        }
        if (tail.isNotEmpty()) {
            output += segmentsFromProgressiveTail(tail)
        }
        return output
    }

    private data class BlockMathMatch(val latex: String, val range: IntRange)

    private fun firstBlockMathMatch(text: String): BlockMathMatch? {
        val patterns = listOf(
            Regex("""\$\$([\s\S]+?)\$\$"""),
            Regex("""\\\[([\s\S]+?)\\\]"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val latex = match.groupValues.getOrNull(1) ?: continue
            return BlockMathMatch(latex = latex, range = match.range)
        }
        return null
    }

    /**
     * Freezes completed paragraphs and block structures while the final paragraph is still growing.
     * Without this, the entire in-flight message stays in one tail segment and renders as plain text.
     */
    private fun splitProgressiveStablePrefix(text: String): List<ChatRichContentSegment> {
        val lastParagraphBreak = text.lastIndexOf("\n\n")
        if (lastParagraphBreak == -1 ||
            (lastParagraphBreak == 0 && !text.substring(2).contains("\n\n"))
        ) {
            return listOf(ChatRichContentSegment.Prose(text))
        }

        val stablePrefix = text.substring(0, lastParagraphBreak + 2)
        val tail = text.substring(lastParagraphBreak + 2)
        val output = mutableListOf<ChatRichContentSegment>()
        if (stablePrefix.isNotBlank()) {
            output += segmentCompletedProseBlocks(stablePrefix)
        }
        if (tail.isNotEmpty()) {
            output += segmentsFromProgressiveTail(tail).ifEmpty {
                listOf(ChatRichContentSegment.RawFragment(tail))
            }
        }
        return output
    }

    private fun segmentCompletedProseBlocks(text: String): List<ChatRichContentSegment> {
        if (text.isEmpty()) return emptyList()

        val structured = segmentsFromProgressiveTail(text, plainAsProse = true)
        if (structured.any { segment -> segment !is ChatRichContentSegment.Prose }) {
            return structured
        }
        if (structured.size > 1) {
            return structured
        }
        if (structured.singleOrNull() is ChatRichContentSegment.Prose) {
            val prose = structured.single() as ChatRichContentSegment.Prose
            if (prose.markdown.lines().any { line ->
                    isMarkdownHeadingLine(line) ||
                        isMarkdownListLine(line) ||
                        isMarkdownBlockquoteLine(line) ||
                        isGfmTableRow(line)
                }
            ) {
                return structured
            }
        }

        if (!text.contains("\n\n")) {
            return listOf(ChatRichContentSegment.Prose(text))
        }

        val endsWithParagraphBreak = text.endsWith("\n\n")
        val core = if (endsWithParagraphBreak) text.dropLast(2) else text
        return core.split("\n\n")
            .filter { it.isNotEmpty() }
            .map { paragraph ->
                ChatRichContentSegment.Prose(
                    if (endsWithParagraphBreak) "$paragraph\n\n" else paragraph,
                )
            }
    }

    /** Pulls complete markdown blocks out of a progressive tail for rich rendering. */
    private fun segmentsFromProgressiveTail(
        tail: String,
        plainAsProse: Boolean = false,
    ): List<ChatRichContentSegment> {
        val lines = tail.split('\n')
        val output = mutableListOf<ChatRichContentSegment>()
        val plainLines = mutableListOf<String>()
        var index = 0

        fun flushPlainLines() {
            if (plainLines.isEmpty()) return
            val text = plainLines.joinToString("\n")
            output += if (plainAsProse) {
                ChatRichContentSegment.Prose(text)
            } else {
                ChatRichContentSegment.RawFragment(text)
            }
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
            incompleteDisplayMathIndex(text),
            incompleteBracketMathIndex(text),
            incompleteInlineLatexIndex(text),
            incompleteParenLatexIndex(text),
        )
        return candidates.minOrNull()
    }

    private fun incompleteBacktickIndex(text: String): Int? {
        var inFence = false
        var inInlineCode = false
        var openIndex: Int? = null
        var index = 0
        while (index < text.length) {
            if (text.startsWith("```", index)) {
                inFence = !inFence
                inInlineCode = false
                openIndex = null
                index += 3
                continue
            }
            if (!inFence && text[index] == '`') {
                if (inInlineCode) {
                    inInlineCode = false
                    openIndex = null
                } else {
                    inInlineCode = true
                    openIndex = index
                }
            }
            index++
        }
        return if (inInlineCode) openIndex else null
    }

    private fun incompleteBracketMathIndex(text: String): Int? {
        var index = 0
        while (index < text.length) {
            if (!text.startsWith("\\[", index)) {
                index++
                continue
            }
            val openStart = index
            val searchStart = index + 2
            if (searchStart >= text.length) return openStart
            val closeIndex = text.indexOf("\\]", searchStart)
            if (closeIndex >= 0) {
                index = closeIndex + 2
                continue
            }
            return openStart
        }
        return null
    }

    private fun incompleteDisplayMathIndex(text: String): Int? {
        var index = 0
        while (index < text.length) {
            if (text[index] != '$') {
                index++
                continue
            }
            val next = index + 1
            if (next >= text.length || text[next] != '$') {
                index++
                continue
            }
            val openStart = index
            val searchStart = next + 1
            if (searchStart >= text.length) return openStart
            val closeIndex = text.indexOf("$$", searchStart)
            if (closeIndex >= 0) {
                index = closeIndex + 2
                continue
            }
            return openStart
        }
        return null
    }

    private fun incompleteInlineLatexIndex(text: String): Int? {
        var inMath = false
        var openIndex: Int? = null
        var index = 0
        while (index < text.length) {
            if (text[index] != '$') {
                index++
                continue
            }
            val next = index + 1
            if (next < text.length && text[next] == '$') {
                val skipped = skipDisplayMath(text, index)
                index = skipped ?: text.length
                continue
            }
            if (isCurrencyDollar(text, index)) {
                index++
                continue
            }
            if (inMath) {
                inMath = false
                openIndex = null
            } else {
                inMath = true
                openIndex = index
            }
            index++
        }
        return if (inMath) openIndex else null
    }

    private fun skipDisplayMath(text: String, openIndex: Int): Int? {
        val searchStart = openIndex + 2
        if (searchStart > text.length) return null
        if (searchStart == text.length) return searchStart
        val closeIndex = text.indexOf("$$", searchStart)
        return if (closeIndex >= 0) closeIndex + 2 else text.length
    }

    private fun isCurrencyDollar(text: String, index: Int): Boolean {
        if (text[index] != '$') return false
        var cursor = index + 1
        if (cursor < text.length && text[cursor] == ' ') {
            cursor++
        }
        if (cursor >= text.length) return false
        return text[cursor].isDigit()
    }

    private fun incompleteParenLatexIndex(text: String): Int? {
        val openPattern = Regex("""\\\(""")
        val closePattern = Regex("""\\\)""")
        val opens = openPattern.findAll(text).map { it.range.first }.toList()
        val closes = closePattern.findAll(text).map { it.range.first }.toList()
        var closeIterator = closes.iterator()
        var nextClose = if (closeIterator.hasNext()) closeIterator.next() else null

        for (open in opens) {
            while (nextClose != null && nextClose < open) {
                nextClose = if (closeIterator.hasNext()) closeIterator.next() else null
            }
            if (nextClose != null && nextClose > open) {
                nextClose = if (closeIterator.hasNext()) closeIterator.next() else null
            } else {
                return open
            }
        }
        return null
    }
}
