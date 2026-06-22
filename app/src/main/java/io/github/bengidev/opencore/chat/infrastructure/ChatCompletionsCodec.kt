package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

internal object ChatCompletionsCodec {
    fun encodeRequest(
        modelId: String,
        messages: List<SidePanelMessage>,
        reasoning: SidePanelReasoningModel
    ): String = buildString {
        append("""{"model":""")
        appendQuoted(modelId)
        append(""","messages":[""")
        messages.forEachIndexed { index, message ->
            if (index > 0) append(',')
            append("""{"role":""")
            appendQuoted(message.role)
            append(""","content":""")
            appendQuoted(message.content)
            append('}')
        }
        append(']')
        reasoning.effort?.let { effort ->
            append(""","reasoning":{"effort":""")
            appendQuoted(effort)
            append('}')
        }
        append('}')
    }

    fun decodeAssistantContent(responseBody: String): String {
        val content = extractJsonString(responseBody, "content")?.trim().orEmpty()
        if (content.isNotEmpty()) return content
        throw ChatCompletionException(parseErrorMessage(responseBody) ?: "Empty assistant response")
    }

    fun parseErrorMessage(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return extractJsonString(responseBody, "message")?.takeIf { it.isNotBlank() }
    }

    private fun StringBuilder.appendQuoted(value: String) {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
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

internal class ChatCompletionException(message: String) : Exception(message)
