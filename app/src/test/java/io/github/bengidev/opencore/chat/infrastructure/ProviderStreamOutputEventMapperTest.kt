package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderStreamOutputEventMapperTest {

    @Test
    fun mapSidebandPayload_execCommandBegin() {
        val payload = """{"type":"exec_command_begin","command":"npm test","cwd":"/tmp/project"}"""
        assertEquals(
            listOf(ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp/project")),
            ProviderStreamOutputEventMapper.mapSidebandPayload(payload),
        )
    }

    @Test
    fun mapSidebandPayload_execCommandOutputDelta() {
        val payload = """{"type":"exec_command_output_delta","chunk":"PASS suite\n"}"""
        assertEquals(
            listOf(ChatStreamingEvent.OutputStreamDelta("PASS suite\n")),
            ProviderStreamOutputEventMapper.mapSidebandPayload(payload),
        )
    }

    @Test
    fun mapSidebandPayload_execCommandEnd() {
        val payload = """{"type":"exec_command_end","status":"completed","exit_code":0,"duration_ms":1200}"""
        assertEquals(
            listOf(
                ChatStreamingEvent.OutputStreamEnded(
                    status = ChatOutputStreamStatus.COMPLETED,
                    exitCode = 0,
                    durationMs = 1200,
                )
            ),
            ProviderStreamOutputEventMapper.mapSidebandPayload(payload),
        )
    }

    @Test
    fun mapSidebandPayload_nonZeroExitWithoutExplicitStatus() {
        val payload = """{"type":"exec_command_end","exit_code":1}"""
        assertEquals(
            listOf(
                ChatStreamingEvent.OutputStreamEnded(
                    status = ChatOutputStreamStatus.FAILED,
                    exitCode = 1,
                    durationMs = null,
                )
            ),
            ProviderStreamOutputEventMapper.mapSidebandPayload(payload),
        )
    }

    @Test
    fun mapSidebandPayload_argvCommandAndNestedEnvelope() {
        val payload = """{"type":"exec_command_begin","msg":{"command":["echo","ok"],"cwd":"/tmp"}}"""
        assertEquals(
            listOf(ChatStreamingEvent.OutputStreamBegan(command = "echo ok", cwd = "/tmp")),
            ProviderStreamOutputEventMapper.mapSidebandPayload(payload),
        )
    }
}
