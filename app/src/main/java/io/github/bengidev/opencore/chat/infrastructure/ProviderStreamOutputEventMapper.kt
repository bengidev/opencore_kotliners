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
        ChatJsonStructureScanner.extractNestedObject(payload, "msg")?.let { nested ->
            ChatJsonStringField.extract(nested, "type")?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        ChatJsonStructureScanner.extractNestedObject(payload, "event")?.let { nested ->
            ChatJsonStringField.extract(nested, "type")?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return ChatJsonStringField.extract(payload, "type")?.takeIf { it.isNotEmpty() }
    }

    private fun resolveBodyJson(payload: String): String =
        ChatJsonStructureScanner.extractNestedObject(payload, "msg")
            ?: ChatJsonStructureScanner.extractNestedObject(payload, "event")
            ?: payload

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
            get() = ChatJsonStructureScanner.parseFlexibleCommand(json, "command", trimResult = true)

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
            get() = ChatOutputStreamStatus.fromProvider(
                ChatJsonStringField.extract(json, "status"),
                resolvedExitCode,
            )
    }

    private class ContentPartEventBody(private val part: ChatStreamContentPart) : EventBody {
        override val resolvedCommand: String? = part.resolvedCommand
        override val resolvedCwd: String? = part.cwd?.trim()?.takeIf { it.isNotEmpty() }
        override val resolvedOutputDelta: String? = part.resolvedOutputDelta
        override val resolvedExitCode: Int? = part.exitCode
        override val resolvedDurationMs: Int? = part.durationMs
        override val resolvedStatus: ChatOutputStreamStatus = part.resolvedStatus
    }
}
