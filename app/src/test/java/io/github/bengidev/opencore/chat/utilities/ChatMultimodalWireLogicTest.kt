package io.github.bengidev.opencore.chat.utilities

import android.graphics.Bitmap
import android.graphics.Color
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ChatMultimodalWireLogicTest {
    @Test
    fun makeContentParts_returnsNullWhenNoVisualMedia() {
        assertNull(
            ChatMultimodalWireLogic.makeContentParts(
                modelText = "hello",
                attachments = emptyList(),
            ),
        )
    }

    @Test
    fun prepareAttachmentsForSend_doesNotPersistVideoWirePayload() {
        val videoFile = File.createTempFile("clip", ".mp4")
        videoFile.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70))
        val attachment = ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.VIDEO,
            filename = "clip.mp4",
            localPath = videoFile.absolutePath,
        )

        val prepared = ChatMultimodalWireLogic.prepareAttachmentsForSend(
            attachments = listOf(attachment),
            modelText = "describe this",
        )

        assertNull(prepared.single().wireVideoDataUrl)
        videoFile.delete()
    }

    @Test
    fun prepareAttachmentsForSend_persistsImageWirePayload() {
        val imageFile = File.createTempFile("photo", ".jpg")
        imageFile.writeBytes(sampleJpegBytes())
        val attachment = ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.IMAGE,
            filename = "photo.jpg",
            localPath = imageFile.absolutePath,
        )

        val prepared = ChatMultimodalWireLogic.prepareAttachmentsForSend(
            attachments = listOf(attachment),
            modelText = "what is this",
        )

        val wireUrl = prepared.single().wireImageDataUrl
        assertTrue(wireUrl?.startsWith("data:image/jpeg;base64,") == true)
        imageFile.delete()
    }

    @Test
    fun makeContentParts_reEncodesVideoFromDiskWhenWirePayloadMissing() {
        val videoFile = File.createTempFile("clip", ".mp4")
        videoFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val attachment = ChatMessageAttachment(
            id = UUID.randomUUID(),
            kind = ChatMessageAttachmentKind.VIDEO,
            filename = "clip.mp4",
            localPath = videoFile.absolutePath,
        )

        val parts = ChatMultimodalWireLogic.makeContentParts("describe", listOf(attachment))

        assertEquals(2, parts?.size)
        assertEquals("text", parts?.first()?.type)
        assertEquals("video_url", parts?.last()?.type)
        assertTrue(parts?.last()?.videoUrl?.startsWith("data:video/mp4;base64,") == true)
        videoFile.delete()
    }

    @Test(expected = ChatAttachmentError.VisualEncodingFailed::class)
    fun makeContentParts_throwsWhenImageFileMissing() {
        val attachment = ChatMessageAttachment(
            kind = ChatMessageAttachmentKind.IMAGE,
            filename = "missing.jpg",
            localPath = "/tmp/does-not-exist-${UUID.randomUUID()}.jpg",
        )

        ChatMultimodalWireLogic.makeContentParts("hello", listOf(attachment))
    }

    private fun sampleJpegBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }
    }
}
