package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.utilities.ChatStreamContentPart

/** Extracts assistant text from polymorphic `delta.content` wire shapes. */
internal object ChatStreamDeltaContentExtractor {
    fun extractContentText(payload: String): String? {
        val keyToken = "\"content\""
        val keyIndex = payload.indexOf(keyToken)
        if (keyIndex < 0) return null

        var index = keyIndex + keyToken.length
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length || payload[index] != ':') return null
        index++
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length) return null

        return when (payload[index]) {
            '"' -> ChatJsonStringField.extract(payload, "content")
            '[' -> extractFromArray(payload, index)
            else -> null
        }
    }

    private fun extractFromArray(payload: String, arrayStart: Int): String? {
        val arrayEnd = findMatchingBracket(payload, arrayStart) ?: return null
        val arrayBody = payload.substring(arrayStart + 1, arrayEnd)
        val joined = extractObjects(arrayBody)
            .mapNotNull { objectJson ->
                ChatStreamContentPart(
                    type = ChatJsonStringField.extract(objectJson, "type"),
                    text = ChatJsonStringField.extract(objectJson, "text")
                ).renderedText
            }
            .joinToString("")
        return joined.takeIf { it.isNotEmpty() }
    }

    private fun extractObjects(arrayBody: String): List<String> {
        val objects = mutableListOf<String>()
        var index = 0
        while (index < arrayBody.length) {
            while (index < arrayBody.length && arrayBody[index].isWhitespace()) index++
            if (index >= arrayBody.length || arrayBody[index] != '{') break
            val end = findMatchingBrace(arrayBody, index) ?: break
            objects += arrayBody.substring(index, end + 1)
            index = end + 1
            while (index < arrayBody.length && arrayBody[index].isWhitespace()) index++
            if (index < arrayBody.length && arrayBody[index] == ',') index++
        }
        return objects
    }

    private fun findMatchingBracket(json: String, openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun findMatchingBrace(json: String, openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }
}
