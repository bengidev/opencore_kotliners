package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatModelInputBuilderTest {
    @Test
    fun modelContent_includesFileTextAndVoiceTranscriptBeforeVisibleText() {
        val attachments = listOf(
            ChatMessageAttachment(
                kind = ChatMessageAttachmentKind.FILE,
                filename = "notes.txt",
                localPath = "/tmp/notes.txt",
                fileTextContent = "hello file",
            ),
            ChatMessageAttachment(
                kind = ChatMessageAttachmentKind.AUDIO,
                filename = "Voice note",
                localPath = "/tmp/voice.m4a",
                speechTranscript = "hello voice",
            ),
        )

        val content = ChatModelInputBuilder.modelContent("visible prompt", attachments)

        assertTrue(content.indexOf("hello file") < content.indexOf("hello voice"))
        assertTrue(content.indexOf("hello voice") < content.indexOf("visible prompt"))
    }

    @Test
    fun modelContent_usesVisibleTextOnlyWhenNoHiddenSections() {
        assertEquals("only text", ChatModelInputBuilder.modelContent("only text", emptyList()))
    }
}
