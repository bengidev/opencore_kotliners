package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.utilities.ChatStreamContentPart

/** Extracts assistant text from polymorphic `delta.content` wire shapes. */
internal object ChatStreamDeltaContentExtractor {
    fun extractContentText(payload: String): String? {
        val parts = extractContentParts(payload)
        if (parts.isNotEmpty()) {
            val joined = parts.mapNotNull { it.renderedText }.joinToString("")
            if (joined.isNotEmpty()) return joined
        }
        return extractLegacyContentText(payload)
    }

    fun extractContentParts(payload: String): List<ChatStreamContentPart> {
        val keyToken = "\"content\""
        val keyIndex = payload.indexOf(keyToken)
        if (keyIndex < 0) return emptyList()

        var index = keyIndex + keyToken.length
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length || payload[index] != ':') return emptyList()
        index++
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length || payload[index] != '[') return emptyList()

        val arrayEnd = findMatchingBracket(payload, index) ?: return emptyList()
        val arrayBody = payload.substring(index + 1, arrayEnd)
        return extractObjects(arrayBody).mapNotNull(::parseContentPart)
    }

    fun extractLegacyContentText(payload: String): String? {
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

    private fun parseContentPart(objectJson: String): ChatStreamContentPart? {
        val type = ChatJsonStringField.extract(objectJson, "type")
        return ChatStreamContentPart(
            type = type,
            text = ChatJsonStringField.extract(objectJson, "text"),
            command = parseFlexibleCommand(objectJson, "command"),
            cwd = ChatJsonStringField.extract(objectJson, "cwd"),
            chunk = ChatJsonStringField.extract(objectJson, "chunk"),
            delta = ChatJsonStringField.extract(objectJson, "delta"),
            output = ChatJsonStringField.extract(objectJson, "output"),
            status = ChatJsonStringField.extract(objectJson, "status"),
            exitCode = ChatJsonIntField.extract(objectJson, "exit_code"),
            durationMs = ChatJsonIntField.extract(objectJson, "duration_ms"),
        )
    }

    private fun parseFlexibleCommand(objectJson: String, key: String): String? {
        val keyToken = "\"$key\""
        val keyIndex = objectJson.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < objectJson.length && objectJson[index].isWhitespace()) index++
        if (index >= objectJson.length || objectJson[index] != ':') return null
        index++
        while (index < objectJson.length && objectJson[index].isWhitespace()) index++
        if (index >= objectJson.length) return null
        return when (objectJson[index]) {
            '"' -> ChatJsonStringField.extract(objectJson, key)
            '[' -> {
                val arrayEnd = findMatchingBracket(objectJson, index) ?: return null
                val arrayBody = objectJson.substring(index + 1, arrayEnd)
                parseQuotedStrings(arrayBody).joinToString(" ").takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }

    private fun extractFromArray(payload: String, arrayStart: Int): String? {
        val arrayEnd = findMatchingBracket(payload, arrayStart) ?: return null
        val arrayBody = payload.substring(arrayStart + 1, arrayEnd)
        val joined = extractObjects(arrayBody)
            .mapNotNull { parseContentPart(it)?.renderedText }
            .joinToString("")
        return joined.takeIf { it.isNotEmpty() }
    }

    private fun parseQuotedStrings(arrayBody: String): List<String> {
        val values = mutableListOf<String>()
        var index = 0
        while (index < arrayBody.length) {
            while (index < arrayBody.length && arrayBody[index].isWhitespace()) index++
            if (index >= arrayBody.length || arrayBody[index] != '"') break
            val start = index
            index++
            var escaped = false
            while (index < arrayBody.length) {
                when (val char = arrayBody[index]) {
                    '"' -> if (!escaped) {
                        values += arrayBody.substring(start + 1, index)
                        index++
                        break
                    } else {
                        escaped = false
                    }
                    '\\' -> escaped = !escaped
                    else -> escaped = false
                }
                index++
            }
            while (index < arrayBody.length && arrayBody[index].isWhitespace()) index++
            if (index < arrayBody.length && arrayBody[index] == ',') index++
        }
        return values
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
