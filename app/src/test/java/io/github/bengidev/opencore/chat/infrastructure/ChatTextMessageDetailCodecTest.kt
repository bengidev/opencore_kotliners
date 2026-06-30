package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChatTextMessageDetailCodecTest {
    @Test
    fun encodeDecode_roundTripsAllAttachmentKinds() {
        val attachments = listOf(
            ChatMessageAttachment(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                kind = ChatMessageAttachmentKind.IMAGE,
                filename = "photo.jpg",
                localPath = "/files/photo.jpg",
                thumbnailJpegData = byteArrayOf(1, 2, 3),
                wireImageDataUrl = "data:image/jpeg;base64,abc",
            ),
            ChatMessageAttachment(
                id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                kind = ChatMessageAttachmentKind.VIDEO,
                filename = "clip.mp4",
                localPath = "/files/clip.mp4",
            ),
            ChatMessageAttachment(
                id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                kind = ChatMessageAttachmentKind.FILE,
                filename = "notes.txt",
                localPath = "/files/notes.txt",
                fileTextContent = "hello file",
            ),
            ChatMessageAttachment(
                id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                kind = ChatMessageAttachmentKind.AUDIO,
                filename = "Voice note",
                localPath = "/files/voice.wav",
                waveformSamples = listOf(0.1f, 0.5f),
                audioDurationSeconds = 3.5,
                speechTranscript = "hello voice",
            ),
        )

        val encoded = ChatTextMessageDetailCodec.encode(
            attachments = attachments,
            modelContent = "visible prompt",
        )
        val decoded = ChatTextMessageDetailCodec.decode(encoded)

        assertEquals("visible prompt", decoded.modelContent)
        assertEquals(4, decoded.attachments.size)
        assertEquals(attachments[0].id, decoded.attachments[0].id)
        assertEquals(ChatMessageAttachmentKind.IMAGE, decoded.attachments[0].kind)
        assertEquals("data:image/jpeg;base64,abc", decoded.attachments[0].wireImageDataUrl)
        assertTrue(decoded.attachments[0].thumbnailJpegData.contentEquals(byteArrayOf(1, 2, 3)))
        assertEquals("hello file", decoded.attachments[2].fileTextContent)
        assertEquals("hello voice", decoded.attachments[3].speechTranscript)
        assertEquals(3.5, decoded.attachments[3].audioDurationSeconds, 0.0)
        assertEquals(listOf(0.1f, 0.5f), decoded.attachments[3].waveformSamples)
    }

    @Test
    fun decode_returnsEmptyDetailForCorruptJson() {
        val decoded = ChatTextMessageDetailCodec.decode("{not valid json")
        assertTrue(decoded.attachments.isEmpty())
        assertNull(decoded.modelContent)
    }

    @Test
    fun encode_returnsNullForEmptyPayload() {
        assertNull(ChatTextMessageDetailCodec.encode(emptyList(), modelContent = null))
        assertNull(ChatTextMessageDetailCodec.encode(emptyList(), modelContent = "   "))
    }
}
