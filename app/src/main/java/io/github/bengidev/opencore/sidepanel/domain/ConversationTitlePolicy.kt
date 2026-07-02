package io.github.bengidev.opencore.sidepanel.domain

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment

/** Derives the display title for a conversation from the latest user message. */
internal object ConversationTitlePolicy {
    const val MAX_LENGTH = 80

    fun fromUserMessage(text: String, attachments: List<ChatMessageAttachment> = emptyList()): String {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            return trimmed.take(MAX_LENGTH)
        }
        val voiceTranscript = attachments
            .mapNotNull { it.speechTranscript?.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return attachments.firstOrNull()?.filename?.take(MAX_LENGTH).orEmpty()
        return if (voiceTranscript.length <= 40) {
            voiceTranscript
        } else {
            voiceTranscript.take(40) + "…"
        }
    }
}
