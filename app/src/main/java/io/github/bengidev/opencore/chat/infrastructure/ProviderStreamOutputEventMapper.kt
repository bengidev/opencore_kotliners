package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.utilities.ChatStreamContentPart

/** Maps provider SSE sideband payloads and command-output content parts into chat events. */
internal object ProviderStreamOutputEventMapper {
    fun mapSidebandPayload(payload: String): List<ChatStreamingEvent>? {
        if (payload.contains("\"choices\"")) return null
        val eventType = resolveEventType(payload)?.lowercase() ?: return null
        val bodyJson = resolveBodyJson(payload)
        return mapEventType(eventType, SidebandEventBody(bodyJson))
    }

    fun mapContentParts(parts: List<ChatStreamContentPart>): List<ChatStreamingEvent> =
        parts.flatMap { part ->
            mapEventType(part.type?.lowercase().orEmpty(), ContentPartEventBody(part)) ?: emptyList()
        }

    private fun mapEventType(
        eventType: String,
        body: EventBody,
    ): List<ChatStreamingEvent>? = when (eventType) {
        "exec_command_begin", "command_output_begin", "command_execution_begin", "output_stream_begin" -> {
            val command = body.resolvedCommand ?: return null
            listOf(ChatStreamingEvent.OutputStreamBegan(command = command, cwd = body.resolvedCwd))
        }
        "exec_command_output_delta", "command_output_delta", "command_execution_output_delta", "output_stream_delta" -> {
            val delta = body.resolvedOutputDelta ?: return null
            listOf(ChatStreamingEvent.OutputStreamDelta(delta))
        }
        "exec_command_end", "command_output_end", "command_execution_end", "output_stream_end" -> {
            listOf(
                ChatStreamingEvent.OutputStreamEnded(
                    status = body.resolvedStatus,
                    exitCode = body.resolvedExitCode,
                    durationMs = body.resolvedDurationMs,
                )
            )
        }
        else -> null
    }

    private fun resolveEventType(payload: String): String? {
        extractNestedObject(payload, "msg")?.let { nested ->
            ChatJsonStringField.extract(nested, "type")?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        extractNestedObject(payload, "event")?.let { nested ->
            ChatJsonStringField.extract(nested, "type")?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return ChatJsonStringField.extract(payload, "type")?.takeIf { it.isNotEmpty() }
    }

    private fun resolveBodyJson(payload: String): String =
        extractNestedObject(payload, "msg")
            ?: extractNestedObject(payload, "event")
            ?: payload

    private fun extractNestedObject(payload: String, key: String): String? {
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

    private interface EventBody {
        val resolvedCommand: String?
        val resolvedCwd: String?
        val resolvedOutputDelta: String?
        val resolvedExitCode: Int?
        val resolvedDurationMs: Int?
        val resolvedStatus: ChatOutputStreamStatus
    }

    private class SidebandEventBody(private val json: String) : EventBody {
        override val resolvedCommand: String?
            get() = parseFlexibleCommand(json, "command")

        override val resolvedCwd: String?
            get() = ChatJsonStringField.extract(json, "cwd")?.trim()?.takeIf { it.isNotEmpty() }

        override val resolvedOutputDelta: String?
            get() = sequenceOf(
                ChatJsonStringField.extract(json, "chunk"),
                ChatJsonStringField.extract(json, "delta"),
                ChatJsonStringField.extract(json, "output"),
                ChatJsonStringField.extract(json, "text"),
            ).firstOrNull { !it.isNullOrEmpty() }

        override val resolvedExitCode: Int?
            get() = ChatJsonIntField.extract(json, "exit_code")

        override val resolvedDurationMs: Int?
            get() = ChatJsonIntField.extract(json, "duration_ms")

        override val resolvedStatus: ChatOutputStreamStatus
            get() = resolveStatus(ChatJsonStringField.extract(json, "status"), resolvedExitCode)
    }

    private class ContentPartEventBody(private val part: ChatStreamContentPart) : EventBody {
        override val resolvedCommand: String? = part.resolvedCommand
        override val resolvedCwd: String? = part.cwd?.trim()?.takeIf { it.isNotEmpty() }
        override val resolvedOutputDelta: String? = part.resolvedOutputDelta
        override val resolvedExitCode: Int? = part.exitCode
        override val resolvedDurationMs: Int? = part.durationMs
        override val resolvedStatus: ChatOutputStreamStatus = part.resolvedStatus
    }

    private fun resolveStatus(raw: String?, exitCode: Int?): ChatOutputStreamStatus {
        val normalized = raw?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "failed", "error", "failure" -> ChatOutputStreamStatus.FAILED
            "completed", "complete", "success", "succeeded", "ok" -> ChatOutputStreamStatus.COMPLETED
            else -> if (exitCode != null && exitCode != 0) {
                ChatOutputStreamStatus.FAILED
            } else {
                ChatOutputStreamStatus.COMPLETED
            }
        }
    }

    private fun parseFlexibleCommand(json: String, key: String): String? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length) return null
        return when (json[index]) {
            '"' -> ChatJsonStringField.extract(json, key)?.trim()?.takeIf { it.isNotEmpty() }
            '[' -> {
                val arrayEnd = findMatchingBracket(json, index) ?: return null
                val arrayBody = json.substring(index + 1, arrayEnd)
                parseQuotedStrings(arrayBody).joinToString(" ").takeIf { it.isNotEmpty() }
            }
            else -> null
        }
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
}
