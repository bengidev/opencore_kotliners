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

        val arrayEnd = ChatJsonStructureScanner.findMatchingBracket(payload, index) ?: return emptyList()
        val arrayBody = payload.substring(index + 1, arrayEnd)
        return ChatJsonStructureScanner.extractObjects(arrayBody).mapNotNull(::parseContentPart)
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
            command = ChatJsonStructureScanner.parseFlexibleCommand(objectJson, "command"),
            cwd = ChatJsonStringField.extract(objectJson, "cwd"),
            chunk = ChatJsonStringField.extract(objectJson, "chunk"),
            delta = ChatJsonStringField.extract(objectJson, "delta"),
            output = ChatJsonStringField.extract(objectJson, "output"),
            status = ChatJsonStringField.extract(objectJson, "status"),
            exitCode = ChatJsonIntField.extract(objectJson, "exit_code"),
            durationMs = ChatJsonIntField.extract(objectJson, "duration_ms"),
        )
    }

    private fun extractFromArray(payload: String, arrayStart: Int): String? {
        val arrayEnd = ChatJsonStructureScanner.findMatchingBracket(payload, arrayStart) ?: return null
        val arrayBody = payload.substring(arrayStart + 1, arrayEnd)
        val joined = ChatJsonStructureScanner.extractObjects(arrayBody)
            .mapNotNull { parseContentPart(it)?.renderedText }
            .joinToString("")
        return joined.takeIf { it.isNotEmpty() }
    }
}
