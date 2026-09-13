package io.github.bengidev.opencore.chat.utilities

/**
 * Hides unclosed markdown delimiters while a response is still streaming so Markwon can render
 * the stable prefix without flashing raw `**`, `` ` ``, or fence markers.
 */
internal object ChatStreamingMarkdownSafeText {
    fun sanitize(text: String): String {
        var result = text
        result = hideUnclosedCodeFence(result)
        result = hideUnclosed(result, "**")
        result = hideUnclosedItalic(result)
        result = hideUnclosedInlineCode(result)
        return result
    }

    private fun hideUnclosedCodeFence(text: String): String {
        val pattern = "```"
        var count = 0
        var index = 0
        var lastFenceStart = -1

        while (index <= text.length - pattern.length) {
            if (text.startsWith(pattern, index)) {
                count++
                lastFenceStart = index
                index += pattern.length
            } else {
                index++
            }
        }

        if (count % 2 == 1 && lastFenceStart != -1) {
            return text.substring(0, lastFenceStart)
        }
        return text
    }

    private fun hideUnclosed(text: String, marker: String): String {
        var count = 0
        var index = 0
        var lastIndex = -1

        while (index <= text.length - marker.length) {
            if (text.startsWith(marker, index)) {
                count++
                lastIndex = index
                index += marker.length
            } else {
                index++
            }
        }

        if (count % 2 == 1 && lastIndex != -1) {
            return text.substring(0, lastIndex)
        }
        return text
    }

    private fun hideUnclosedItalic(text: String): String {
        var count = 0
        var lastSingleIndex = -1
        var index = 0

        while (index < text.length) {
            if (text[index] == '*') {
                val previousIsStar = index > 0 && text[index - 1] == '*'
                val nextIsStar = index < text.length - 1 && text[index + 1] == '*'
                if (!previousIsStar && !nextIsStar) {
                    count++
                    lastSingleIndex = index
                } else if (!previousIsStar && nextIsStar) {
                    index++
                }
            }
            index++
        }

        if (count % 2 == 1 && lastSingleIndex != -1) {
            return text.substring(0, lastSingleIndex)
        }
        return text
    }

    private fun hideUnclosedInlineCode(text: String): String {
        var count = 0
        var lastSingleIndex = -1
        var index = 0

        while (index < text.length) {
            if (text[index] == '`') {
                val isTripleStart = index <= text.length - 3 && text.startsWith("```", index)
                val isTripleMiddle = index > 0 && index < text.length - 1 &&
                    text[index - 1] == '`' && text[index + 1] == '`'
                val isTripleEnd = index >= 2 && text.substring(index - 2, index + 1) == "```"
                if (!isTripleStart && !isTripleMiddle && !isTripleEnd) {
                    count++
                    lastSingleIndex = index
                } else if (isTripleStart) {
                    index += 2
                }
            }
            index++
        }

        if (count % 2 == 1 && lastSingleIndex != -1) {
            return text.substring(0, lastSingleIndex)
        }
        return text
    }
}
