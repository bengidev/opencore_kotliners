package io.github.bengidev.opencore.chat.infrastructure

import android.util.Base64
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.domain.ChatTextMessageDetail
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

internal object ChatTextMessageDetailCodec {
    fun encode(attachments: List<ChatMessageAttachment>, modelContent: String?): String? {
        if (attachments.isEmpty() && modelContent.isNullOrBlank()) return null
        return buildString {
            append('{')
            append("\"attachments\":[")
            attachments.forEachIndexed { index, attachment ->
                if (index > 0) append(',')
                appendAttachment(attachment)
            }
            append(']')
            if (!modelContent.isNullOrBlank()) {
                append(",\"modelContent\":")
                appendQuoted(modelContent)
            }
            append('}')
        }
    }

    fun decode(json: String?): ChatTextMessageDetail {
        if (json.isNullOrBlank()) return ChatTextMessageDetail()
        return runCatching {
            val attachments = parseAttachments(json)
            val modelContent = ChatJsonStringField.extract(json, "modelContent")
            ChatTextMessageDetail(attachments = attachments, modelContent = modelContent)
        }.getOrDefault(ChatTextMessageDetail())
    }

    private fun parseAttachments(json: String): List<ChatMessageAttachment> {
        val keyToken = "\"attachments\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return emptyList()
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return emptyList()
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '[') return emptyList()
        index++

        val attachments = mutableListOf<ChatMessageAttachment>()
        while (index < json.length) {
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] == ']') break
            if (json[index] != '{') {
                index++
                continue
            }
            val end = findMatchingBrace(json, index) ?: break
            attachments += parseAttachment(json.substring(index, end + 1))
            index = end + 1
            while (index < json.length && json[index].isWhitespace()) index++
            if (index < json.length && json[index] == ',') index++
        }
        return attachments
    }

    private fun parseAttachment(objectJson: String): ChatMessageAttachment {
        val id = ChatJsonStringField.extract(objectJson, "id")?.let(UUID::fromString) ?: UUID.randomUUID()
        val kindWire = ChatJsonStringField.extract(objectJson, "kind").orEmpty()
        val kind = when (kindWire.lowercase()) {
            "image" -> ChatMessageAttachmentKind.IMAGE
            "video" -> ChatMessageAttachmentKind.VIDEO
            "file" -> ChatMessageAttachmentKind.FILE
            "audio" -> ChatMessageAttachmentKind.AUDIO
            else -> ChatMessageAttachmentKind.FILE
        }
        return ChatMessageAttachment(
            id = id,
            kind = kind,
            filename = ChatJsonStringField.extract(objectJson, "filename").orEmpty(),
            localPath = ChatJsonStringField.extract(objectJson, "localPath").orEmpty(),
            thumbnailJpegData = ChatJsonStringField.extract(objectJson, "thumbnailJPEGData")
                ?.let { Base64.decode(it, Base64.DEFAULT) },
            waveformSamples = parseFloatArray(objectJson, "waveformSamples"),
            audioDurationSeconds = ChatJsonDoubleField.extract(objectJson, "audioDuration") ?: 0.0,
            speechTranscript = ChatJsonStringField.extract(objectJson, "speechTranscript"),
            fileTextContent = ChatJsonStringField.extract(objectJson, "fileTextContent"),
            wireImageDataUrl = ChatJsonStringField.extract(objectJson, "wireImageDataURL"),
            wireVideoDataUrl = ChatJsonStringField.extract(objectJson, "wireVideoDataURL"),
        )
    }

    private fun parseFloatArray(json: String, key: String): List<Float> {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return emptyList()
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return emptyList()
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '[') return emptyList()
        index++

        val values = mutableListOf<Float>()
        while (index < json.length) {
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] == ']') break
            val start = index
            if (json[index] == '-') index++
            while (index < json.length && (json[index].isDigit() || json[index] == '.')) index++
            if (start < index) {
                json.substring(start, index).toFloatOrNull()?.let(values::add)
            }
            while (index < json.length && json[index].isWhitespace()) index++
            if (index < json.length && json[index] == ',') index++
        }
        return values
    }

    private fun StringBuilder.appendAttachment(attachment: ChatMessageAttachment) {
        append('{')
        append("\"id\":")
        appendQuoted(attachment.id.toString())
        append(",\"kind\":")
        appendQuoted(attachment.kind.wireValue)
        append(",\"filename\":")
        appendQuoted(attachment.filename)
        append(",\"localPath\":")
        appendQuoted(attachment.localPath)
        attachment.thumbnailJpegData?.let { data ->
            append(",\"thumbnailJPEGData\":")
            appendQuoted(Base64.encodeToString(data, Base64.NO_WRAP))
        }
        if (attachment.waveformSamples.isNotEmpty()) {
            append(",\"waveformSamples\":[")
            attachment.waveformSamples.forEachIndexed { index, sample ->
                if (index > 0) append(',')
                append(sample)
            }
            append(']')
        }
        if (attachment.audioDurationSeconds > 0) {
            append(",\"audioDuration\":")
            append(attachment.audioDurationSeconds)
        }
        attachment.speechTranscript?.let {
            append(",\"speechTranscript\":")
            appendQuoted(it)
        }
        attachment.fileTextContent?.let {
            append(",\"fileContent\":")
            appendQuoted(it)
            append(",\"fileTextContent\":")
            appendQuoted(it)
        }
        attachment.wireImageDataUrl?.let {
            append(",\"wireImageDataURL\":")
            appendQuoted(it)
        }
        attachment.wireVideoDataUrl?.let {
            append(",\"wireVideoDataURL\":")
            appendQuoted(it)
        }
        append('}')
    }

    private fun StringBuilder.appendQuoted(value: String) {
        ChatJsonStringField.appendQuoted(this, value)
    }

    private fun findMatchingBrace(json: String, openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }
}

private val ChatMessageAttachmentKind.wireValue: String
    get() = when (this) {
        ChatMessageAttachmentKind.IMAGE -> "image"
        ChatMessageAttachmentKind.VIDEO -> "video"
        ChatMessageAttachmentKind.FILE -> "file"
        ChatMessageAttachmentKind.AUDIO -> "audio"
    }

internal fun SidePanelMessage.textMessageDetail(): ChatTextMessageDetail =
    ChatTextMessageDetailCodec.decode(detailJson)

internal fun SidePanelMessage.providerContent(): String =
    textMessageDetail().modelContent?.takeIf { it.isNotBlank() } ?: content

internal fun SidePanelMessage.attachments(): List<ChatMessageAttachment> =
    textMessageDetail().attachments
