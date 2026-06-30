package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.shared.providers.ProviderChatContentPart
import java.io.File

/** Builds OpenRouter multimodal `messages[].content` parts from composer attachments. */
internal object ChatMultimodalWireLogic {
    fun hasVisualMedia(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any { it.kind == ChatMessageAttachmentKind.IMAGE || it.kind == ChatMessageAttachmentKind.VIDEO }

    fun hasPersistedVisualWire(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any {
            (it.kind == ChatMessageAttachmentKind.IMAGE && it.wireImageDataUrl != null) ||
                (it.kind == ChatMessageAttachmentKind.VIDEO && it.wireVideoDataUrl != null)
        }

    fun prepareAttachmentsForSend(
        attachments: List<ChatMessageAttachment>,
        modelText: String,
    ): List<ChatMessageAttachment> {
        if (!hasVisualMedia(attachments)) return attachments

        val prepared = attachments.toMutableList()
        val parts = mutableListOf<ProviderChatContentPart>()

        val trimmedModelText = modelText.trim()
        if (trimmedModelText.isNotEmpty()) {
            parts += ProviderChatContentPart.text(trimmedModelText)
        }

        prepared.forEachIndexed { index, attachment ->
            when (attachment.kind) {
                ChatMessageAttachmentKind.IMAGE -> {
                    val dataUrl = imageDataUrl(attachment)
                    prepared[index] = attachment.withWirePayloads(imageDataUrl = dataUrl)
                    parts += ProviderChatContentPart.imageUrl(dataUrl)
                }
                ChatMessageAttachmentKind.VIDEO -> {
                    val dataUrl = videoDataUrl(attachment)
                    prepared[index] = attachment.withWirePayloads(videoDataUrl = dataUrl)
                    parts += ProviderChatContentPart.videoUrl(dataUrl)
                }
                ChatMessageAttachmentKind.FILE, ChatMessageAttachmentKind.AUDIO -> Unit
            }
        }

        val visualPartCount = parts.count { it.type == "image_url" || it.type == "video_url" }
        val expectedVisualCount = attachments.count {
            it.kind == ChatMessageAttachmentKind.IMAGE || it.kind == ChatMessageAttachmentKind.VIDEO
        }
        if (visualPartCount != expectedVisualCount || parts.isEmpty()) {
            throw ChatAttachmentError.VisualEncodingFailed("attachment")
        }

        return prepared
    }

    fun makeContentParts(
        modelText: String,
        attachments: List<ChatMessageAttachment>,
    ): List<ProviderChatContentPart>? {
        if (hasPersistedVisualWire(attachments)) {
            return makeContentPartsFromPersisted(modelText, attachments)
        }
        if (!hasVisualMedia(attachments)) return null

        val parts = mutableListOf<ProviderChatContentPart>()
        val trimmedModelText = modelText.trim()
        if (trimmedModelText.isNotEmpty()) {
            parts += ProviderChatContentPart.text(trimmedModelText)
        }

        attachments.filter { it.kind == ChatMessageAttachmentKind.IMAGE }.forEach { attachment ->
            parts += ProviderChatContentPart.imageUrl(imageDataUrl(attachment))
        }
        attachments.filter { it.kind == ChatMessageAttachmentKind.VIDEO }.forEach { attachment ->
            parts += ProviderChatContentPart.videoUrl(videoDataUrl(attachment))
        }

        val visualPartCount = parts.count { it.type == "image_url" || it.type == "video_url" }
        val expectedVisualCount = attachments.count {
            it.kind == ChatMessageAttachmentKind.IMAGE || it.kind == ChatMessageAttachmentKind.VIDEO
        }
        if (visualPartCount != expectedVisualCount) {
            throw ChatAttachmentError.VisualEncodingFailed("attachment")
        }

        return parts.takeIf { it.isNotEmpty() }
    }

    fun makeContentPartsFromPersisted(
        modelText: String,
        attachments: List<ChatMessageAttachment>,
    ): List<ProviderChatContentPart>? {
        if (!hasVisualMedia(attachments)) return null

        val parts = mutableListOf<ProviderChatContentPart>()
        val trimmedModelText = modelText.trim()
        if (trimmedModelText.isNotEmpty()) {
            parts += ProviderChatContentPart.text(trimmedModelText)
        }

        attachments.filter { it.kind == ChatMessageAttachmentKind.IMAGE }.forEach { attachment ->
            val dataUrl = attachment.wireImageDataUrl
                ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
            parts += ProviderChatContentPart.imageUrl(dataUrl)
        }
        attachments.filter { it.kind == ChatMessageAttachmentKind.VIDEO }.forEach { attachment ->
            val dataUrl = attachment.wireVideoDataUrl
                ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
            parts += ProviderChatContentPart.videoUrl(dataUrl)
        }

        return parts.takeIf { it.isNotEmpty() }
    }

    fun estimatedWireTokenOverhead(attachments: List<ChatMessageAttachment>): Int =
        attachments.sumOf { attachment ->
            val payloadLength = when (attachment.kind) {
                ChatMessageAttachmentKind.IMAGE ->
                    attachment.wireImageDataUrl?.length ?: fileByteCount(attachment.localPath) * 4 / 3
                ChatMessageAttachmentKind.VIDEO ->
                    attachment.wireVideoDataUrl?.length ?: fileByteCount(attachment.localPath) * 4 / 3
                ChatMessageAttachmentKind.FILE, ChatMessageAttachmentKind.AUDIO -> 0
            }
            maxOf((payloadLength + 3) / 4, 0)
        }

    private fun imageDataUrl(attachment: ChatMessageAttachment): String {
        attachment.wireImageDataUrl?.let { return it }
        return ChatMultimodalImagePayloadLogic.dataUrlFromFile(attachment.localPath)
            ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
    }

    private fun videoDataUrl(attachment: ChatMessageAttachment): String {
        attachment.wireVideoDataUrl?.let { return it }
        val byteCount = fileByteCount(attachment.localPath)
        ChatAttachmentSizeLimits.validateVideoWireSize(byteCount)
        return ChatMultimodalVideoPayloadLogic.dataUrlFromFile(attachment.localPath)
            ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
    }

    private fun fileByteCount(localPath: String): Int =
        runCatching { File(localPath).length().toInt() }.getOrDefault(0)
}
