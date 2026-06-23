package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent

internal object ChatStreamingCodec {
    fun mapDataPayload(payload: String): List<ChatStreamingEvent>? {
        ChatCompletionsCodec.parseErrorMessage(payload)?.let { message ->
            return listOf(ChatStreamingEvent.Error(ChatStreamError(message)))
        }

        val events = mutableListOf<ChatStreamingEvent>()
        val reasoning = extractJsonString(payload, "reasoning")
            ?: extractJsonString(payload, "reasoning_content")
        if (!reasoning.isNullOrBlank() && reasoning.trim().isNotEmpty()) {
            events += ChatStreamingEvent.ThinkingDelta(reasoning)
        }
        extractJsonString(payload, "content")
            ?.takeIf { it.isNotEmpty() }
            ?.let { events += ChatStreamingEvent.TextDelta(it) }
        return events.takeIf { it.isNotEmpty() }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val keyToken = "\"$key\""
        var searchFrom = 0
        while (true) {
            val keyIndex = json.indexOf(keyToken, searchFrom)
            if (keyIndex < 0) return null
            var index = keyIndex + keyToken.length
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != ':') {
                searchFrom = keyIndex + 1
                continue
            }
            index++
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != '"') {
                searchFrom = keyIndex + 1
                continue
            }
            index++
            val output = StringBuilder()
            while (index < json.length) {
                when (val char = json[index]) {
                    '"' -> return output.toString()
                    '\\' -> {
                        index++
                        if (index >= json.length) return null
                        output.append(
                            when (val escaped = json[index]) {
                                '"', '\\', '/' -> escaped
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                'u' -> {
                                    if (index + 4 >= json.length) return null
                                    val codePoint = json.substring(index + 1, index + 5).toInt(16)
                                    index += 4
                                    codePoint.toChar()
                                }
                                else -> escaped
                            }
                        )
                    }
                    else -> output.append(char)
                }
                index++
            }
            return null
        }
    }
}
