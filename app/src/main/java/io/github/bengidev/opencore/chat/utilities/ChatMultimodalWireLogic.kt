package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.shared.providers.ProviderChatContentPart
import java.io.File

/** Builds OpenRouter multimodal `messages[].content` parts from composer attachments. */
internal object ChatMultimodalWireLogic {
    fun hasVisualMedia(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any { it.kind == ChatMessageAttachmentKind.IMAGE || it.kind == ChatMessageAttachmentKind.VIDEO }

    fun hasPersistedImageWire(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any {
            it.kind == ChatMessageAttachmentKind.IMAGE && it.wireImageDataUrl != null
        }

    fun prepareAttachmentsForSend(
        attachments: List<ChatMessageAttachment>,
        modelText: String,
    ): List<ChatMessageAttachment> {
        if (!hasVisualMedia(attachments)) return attachments

        buildVisualContentParts(
            modelText = modelText,
            attachments = attachments,
            preferPersistedImageWire = false,
        )

        val prepared = attachments.toMutableList()
        prepared.forEachIndexed { index, attachment ->
            when (attachment.kind) {
                ChatMessageAttachmentKind.IMAGE -> {
                    val dataUrl = imageDataUrl(attachment)
                    prepared[index] = attachment.withWirePayloads(imageDataUrl = dataUrl)
                }
                ChatMessageAttachmentKind.VIDEO -> {
                    videoDataUrl(attachment)
                }
                ChatMessageAttachmentKind.FILE, ChatMessageAttachmentKind.AUDIO -> Unit
            }
        }
        return prepared
    }

    fun makeContentParts(
        modelText: String,
        attachments: List<ChatMessageAttachment>,
    ): List<ProviderChatContentPart>? {
        if (!hasVisualMedia(attachments) && !hasPersistedImageWire(attachments)) return null

        val parts = buildVisualContentParts(
            modelText = modelText,
            attachments = attachments,
            preferPersistedImageWire = hasPersistedImageWire(attachments),
        )
        return parts.takeIf { it.isNotEmpty() }
    }

    fun estimatedWireTokenOverhead(attachments: List<ChatMessageAttachment>): Int =
        attachments.sumOf { attachment ->
            val payloadLength = when (attachment.kind) {
                ChatMessageAttachmentKind.IMAGE ->
                    attachment.wireImageDataUrl?.length ?: fileByteCount(attachment.localPath) * 4 / 3
                ChatMessageAttachmentKind.VIDEO ->
                    fileByteCount(attachment.localPath) * 4 / 3
                ChatMessageAttachmentKind.FILE, ChatMessageAttachmentKind.AUDIO -> 0
            }
            maxOf((payloadLength + 3) / 4, 0)
        }

    private fun buildVisualContentParts(
        modelText: String,
        attachments: List<ChatMessageAttachment>,
        preferPersistedImageWire: Boolean,
    ): List<ProviderChatContentPart> {
        val parts = mutableListOf<ProviderChatContentPart>()
        val trimmedModelText = modelText.trim()
        if (trimmedModelText.isNotEmpty()) {
            parts += ProviderChatContentPart.text(trimmedModelText)
        }

        attachments.forEach { attachment ->
            when (attachment.kind) {
                ChatMessageAttachmentKind.IMAGE -> {
                    val dataUrl = if (preferPersistedImageWire) {
                        attachment.wireImageDataUrl ?: imageDataUrl(attachment)
                    } else {
                        imageDataUrl(attachment)
                    }
                    parts += ProviderChatContentPart.imageUrl(dataUrl)
                }
                ChatMessageAttachmentKind.VIDEO -> {
                    parts += ProviderChatContentPart.videoUrl(videoDataUrl(attachment))
                }
                ChatMessageAttachmentKind.FILE, ChatMessageAttachmentKind.AUDIO -> Unit
            }
        }
        return parts
    }

    private fun imageDataUrl(attachment: ChatMessageAttachment): String {
        attachment.wireImageDataUrl?.let { return it }
        return ChatMultimodalImagePayloadLogic.dataUrlFromFile(attachment.localPath)
            ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
    }

    private fun videoDataUrl(attachment: ChatMessageAttachment): String {
        val byteCount = fileByteCount(attachment.localPath)
        ChatAttachmentSizeLimits.validateVideoWireSize(byteCount)
        return ChatMultimodalVideoPayloadLogic.dataUrlFromFile(attachment.localPath)
            ?: throw ChatAttachmentError.VisualEncodingFailed(attachment.filename)
    }

    private fun fileByteCount(localPath: String): Int =
        runCatching { File(localPath).length().toInt() }.getOrDefault(0)
}
