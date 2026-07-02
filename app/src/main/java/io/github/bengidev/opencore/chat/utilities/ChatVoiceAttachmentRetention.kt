package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant

/** Retention rules for persisted voice-note audio files. */
internal object ChatVoiceAttachmentRetention {
    /** Voice-note audio is removed one week after the message was sent. */
    private const val RETENTION_SECONDS: Long = 7L * 24L * 60L * 60L

    fun expirationCutoff(from: Instant = Instant.now()): Instant =
        from.minusSeconds(RETENTION_SECONDS)

    fun expireVoiceAttachments(
        messages: List<SidePanelMessage>,
        cutoff: Instant,
    ): Pair<List<SidePanelMessage>, List<String>> {
        val removedPaths = mutableListOf<String>()
        val updated = messages.map { message ->
            val (updatedMessage, paths) = expireVoiceAttachments(message, cutoff)
            removedPaths += paths
            updatedMessage
        }
        return updated to removedPaths
    }

    fun expireVoiceAttachments(
        message: SidePanelMessage,
        cutoff: Instant,
    ): Pair<SidePanelMessage, List<String>> {
        if (message.createdAt >= cutoff) return message to emptyList()

        val detail = ChatTextMessageDetailCodec.decode(message.detailJson)
        if (detail.attachments.isEmpty()) return message to emptyList()

        val removedPaths = mutableListOf<String>()
        val keptAttachments = mutableListOf<ChatMessageAttachment>()
        var promotedTranscript: String? = null

        detail.attachments.forEach { attachment ->
            if (
                attachment.kind == ChatMessageAttachmentKind.AUDIO &&
                !attachment.speechTranscript.isNullOrBlank()
            ) {
                removedPaths += attachment.localPath
                val transcript = attachment.speechTranscript.trim()
                if (message.content.trim().isEmpty() && transcript.isNotEmpty()) {
                    promotedTranscript = transcript
                }
            } else {
                keptAttachments += attachment
            }
        }

        if (removedPaths.isEmpty()) return message to emptyList()

        val updatedContent = promotedTranscript ?: message.content
        val updatedDetailJson = ChatTextMessageDetailCodec.encode(
            attachments = keptAttachments,
            modelContent = detail.modelContent,
        )
        return message.copy(
            content = updatedContent,
            detailJson = updatedDetailJson,
        ) to removedPaths
    }
}
