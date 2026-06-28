package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatJsonStructureScannerTest {

    @Test
    fun findMatchingBracket_respectsQuotedStrings() {
        val json = """["a", "[not a bracket]"]"""
        val end = ChatJsonStructureScanner.findMatchingBracket(json, 0)
        assertEquals(json.lastIndex, end)
    }

    @Test
    fun findMatchingBrace_respectsQuotedStrings() {
        val json = """{"key": "{not a brace}"}"""
        val end = ChatJsonStructureScanner.findMatchingBrace(json, 0)
        assertEquals(json.lastIndex, end)
    }

    @Test
    fun parseQuotedStrings_readsMultipleValues() {
        val values = ChatJsonStructureScanner.parseQuotedStrings("\"one\", \"two\"")
        assertEquals(listOf("one", "two"), values)
    }

    @Test
    fun parseFlexibleCommand_stringValue() {
        val command = ChatJsonStructureScanner.parseFlexibleCommand(
            """{"command":"npm test"}""",
            "command",
        )
        assertEquals("npm test", command)
    }

    @Test
    fun parseFlexibleCommand_arrayValue() {
        val command = ChatJsonStructureScanner.parseFlexibleCommand(
            """{"command":["npm","test"]}""",
            "command",
        )
        assertEquals("npm test", command)
    }

    @Test
    fun extractNestedObject_readsNestedBody() {
        val nested = ChatJsonStructureScanner.extractNestedObject(
            """{"msg":{"type":"output_stream_begin","command":"ls"}}""",
            "msg",
        )
        assertEquals("""{"type":"output_stream_begin","command":"ls"}""", nested)
    }

    @Test
    fun extractObjects_readsTopLevelObjects() {
        val objects = ChatJsonStructureScanner.extractObjects(
            """{"type":"text","text":"Hi"},{"type":"text","text":"!"}""",
        )
        assertEquals(2, objects.size)
    }
}

class ChatOutputStreamDetailCodecTest {

    @Test
    fun encodeDecode_roundTrip() {
        val detail = ChatOutputStreamDetail(
            status = ChatOutputStreamStatus.COMPLETED,
            outputTail = "line one\nline two",
            cwd = "/tmp",
            exitCode = 0,
            durationMs = 500,
        )
        val decoded = ChatOutputStreamDetailCodec.decode(
            ChatOutputStreamDetailCodec.encode(detail),
            isComplete = true,
        )
        assertEquals(detail, decoded)
    }

    @Test
    fun decode_unknownWireValue_usesRunningWhenIncomplete() {
        val detail = ChatOutputStreamDetailCodec.decode(
            """{"status":"mystery","outputTail":"x"}""",
            isComplete = false,
        )
        assertEquals(ChatOutputStreamStatus.RUNNING, detail.status)
        assertEquals("x", detail.outputTail)
    }

    @Test
    fun decode_unknownWireValue_usesCompletedWhenComplete() {
        val detail = ChatOutputStreamDetailCodec.decode(
            """{"status":"mystery","outputTail":"x"}""",
            isComplete = true,
        )
        assertEquals(ChatOutputStreamStatus.COMPLETED, detail.status)
    }

    @Test
    fun decode_blankJson_fallsBackToRunningOrCompleted() {
        assertEquals(
            ChatOutputStreamStatus.RUNNING,
            ChatOutputStreamDetailCodec.decode(null, isComplete = false).status,
        )
        assertEquals(
            ChatOutputStreamStatus.COMPLETED,
            ChatOutputStreamDetailCodec.decode("", isComplete = true).status,
        )
    }
}
