package io.github.bengidev.opencore.chat.infrastructure

/** Shared JSON structure scanning for lightweight stream payload parsing. */
internal object ChatJsonStructureScanner {
    fun findMatchingBracket(json: String, openIndex: Int): Int? {
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

    fun findMatchingBrace(json: String, openIndex: Int): Int? {
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

    fun parseQuotedStrings(arrayBody: String): List<String> {
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

    fun parseFlexibleCommand(json: String, key: String, trimResult: Boolean = false): String? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length) return null
        val value = when (json[index]) {
            '"' -> ChatJsonStringField.extract(json, key)
            '[' -> {
                val arrayEnd = findMatchingBracket(json, index) ?: return null
                val arrayBody = json.substring(index + 1, arrayEnd)
                parseQuotedStrings(arrayBody).joinToString(" ").takeIf { it.isNotEmpty() }
            }
            else -> null
        } ?: return null
        return if (trimResult) value.trim().takeIf { it.isNotEmpty() } else value
    }

    fun extractObjects(arrayBody: String): List<String> {
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

    fun extractNestedObject(payload: String, key: String): String? {
        val keyToken = "\"$key\""
        val keyIndex = payload.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length || payload[index] != ':') return null
        index++
        while (index < payload.length && payload[index].isWhitespace()) index++
        if (index >= payload.length || payload[index] != '{') return null
        val end = findMatchingBrace(payload, index) ?: return null
        return payload.substring(index, end + 1)
    }
}
