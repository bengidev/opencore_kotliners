package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatOutputStreamHumanizerTest {

    @Test
    fun humanize_unwrapsShellWrappedCommands() {
        val info = ChatOutputStreamHumanizer.humanize(
            "/usr/bin/bash -lc \"cd /tmp/project && npm install\"",
            isRunning = true,
        )
        assertEquals("Running", info.verb)
        assertEquals("npm install", info.target)
    }

    @Test
    fun humanize_readCommands() {
        val running = ChatOutputStreamHumanizer.humanize(
            "nl -ba app/src/main/ChatView.kt",
            isRunning = true,
        )
        assertEquals("Reading", running.verb)
        assertEquals("main/ChatView.kt", running.target)

        val completed = ChatOutputStreamHumanizer.humanize(
            "nl -ba app/src/main/ChatView.kt",
            isRunning = false,
        )
        assertEquals("Read", completed.verb)
    }

    @Test
    fun humanize_searchCommands() {
        val info = ChatOutputStreamHumanizer.humanize(
            "rg -n \"streaming\" app",
            isRunning = false,
        )
        assertEquals("Searched", info.verb)
        assertEquals("for streaming in app", info.target)
    }
}

class ChatOutputStreamDetailTest {

    @Test
    fun appendOutput_trimsOutputTailToMaxLines() {
        var detail = ChatOutputStreamDetail()
        val lines = (1..40).joinToString("\n") { "line $it" }
        detail = detail.appendOutput(lines)

        val keptLines = detail.outputTail.lines()
        assertEquals(ChatOutputStreamDetail.MAX_OUTPUT_LINES, keptLines.size)
        assertEquals("line 11", keptLines.first())
        assertEquals("line 40", keptLines.last())
    }
}
