package io.github.bengidev.opencore.chat.domain

import java.util.UUID

/** Persisted attachment shown in the user bubble. Model-facing text lives on the parent message. */
internal data class ChatMessageAttachment(
    val id: UUID = UUID.randomUUID(),
    val kind: ChatMessageAttachmentKind,
    val filename: String,
    /** Absolute path to the copied attachment in app storage. */
    val localPath: String,
    val thumbnailJpegData: ByteArray? = null,
    val waveformSamples: List<Float> = emptyList(),
    val audioDurationSeconds: Double = 0.0,
    /** Speech transcript used only for model input; never shown in the bubble. */
    val speechTranscript: String? = null,
    /** UTF-8 text extracted from plain-text file attachments for model input. */
    val fileTextContent: String? = null,
    /** Base64 data URL persisted at send time so history requests do not re-encode from disk. */
    val wireImageDataUrl: String? = null,
    val wireVideoDataUrl: String? = null,
) {
    fun withWirePayloads(
        imageDataUrl: String? = null,
        videoDataUrl: String? = null,
    ): ChatMessageAttachment = copy(
        wireImageDataUrl = imageDataUrl ?: wireImageDataUrl,
        wireVideoDataUrl = videoDataUrl ?: wireVideoDataUrl,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessageAttachment) return false
        return id == other.id &&
            kind == other.kind &&
            filename == other.filename &&
            localPath == other.localPath &&
            thumbnailJpegData.contentEquals(other.thumbnailJpegData) &&
            waveformSamples == other.waveformSamples &&
            audioDurationSeconds == other.audioDurationSeconds &&
            speechTranscript == other.speechTranscript &&
            fileTextContent == other.fileTextContent &&
            wireImageDataUrl == other.wireImageDataUrl &&
            wireVideoDataUrl == other.wireVideoDataUrl
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + localPath.hashCode()
        result = 31 * result + (thumbnailJpegData?.contentHashCode() ?: 0)
        result = 31 * result + waveformSamples.hashCode()
        result = 31 * result + audioDurationSeconds.hashCode()
        result = 31 * result + (speechTranscript?.hashCode() ?: 0)
        result = 31 * result + (fileTextContent?.hashCode() ?: 0)
        result = 31 * result + (wireImageDataUrl?.hashCode() ?: 0)
        result = 31 * result + (wireVideoDataUrl?.hashCode() ?: 0)
        return result
    }
}

internal data class ChatTextMessageDetail(
    val attachments: List<ChatMessageAttachment> = emptyList(),
    val modelContent: String? = null,
)
