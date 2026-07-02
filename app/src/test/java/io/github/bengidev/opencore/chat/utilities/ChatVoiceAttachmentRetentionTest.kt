package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ChatVoiceAttachmentRetentionTest {
    @Test
    fun expiresOldVoiceAttachments() {
        val cutoff = Instant.ofEpochSecond(1_000_000)
        val message = messageWithAudio(
            createdAt = cutoff.minusSeconds(60),
            content = "",
            localPath = "/tmp/voice.wav",
            speechTranscript = "Expired note",
        )

        val (updated, removedPaths) = ChatVoiceAttachmentRetention.expireVoiceAttachments(message, cutoff)

        assertTrue(ChatTextMessageDetailCodec.decode(updated.detailJson).attachments.isEmpty())
        assertEquals("Expired note", updated.content)
        assertEquals(listOf("/tmp/voice.wav"), removedPaths)
    }

    @Test
    fun keepsRecentVoiceAttachments() {
        val cutoff = Instant.ofEpochSecond(1_000_000)
        val message = messageWithAudio(
            createdAt = cutoff.plusSeconds(60),
            content = "",
            localPath = "/tmp/voice.wav",
            speechTranscript = "Fresh note",
        )

        val (updated, removedPaths) = ChatVoiceAttachmentRetention.expireVoiceAttachments(message, cutoff)

        assertEquals(1, ChatTextMessageDetailCodec.decode(updated.detailJson).attachments.size)
        assertTrue(updated.content.isEmpty())
        assertTrue(removedPaths.isEmpty())
    }

    private fun messageWithAudio(
        createdAt: Instant,
        content: String,
        localPath: String,
        speechTranscript: String,
    ): SidePanelMessage {
        val attachment = ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.AUDIO,
            filename = "Voice note",
            localPath = localPath,
            speechTranscript = speechTranscript,
        )
        return SidePanelMessage(
            id = UUID.randomUUID(),
            role = "user",
            content = content,
            createdAt = createdAt,
            kind = SidePanelMessageKind.TEXT,
            detailJson = ChatTextMessageDetailCodec.encode(listOf(attachment), modelContent = null),
        )
    }
}
