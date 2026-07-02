package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind

/** Builds provider-facing text from visible composer content and hidden attachment metadata. */
internal object ChatModelInputBuilder {
    fun modelContent(visibleText: String, attachments: List<ChatMessageAttachment>): String =
        textSections(visibleText, attachments).joinToString("\n\n")

    fun textSections(visibleText: String, attachments: List<ChatMessageAttachment>): List<String> {
        val sections = mutableListOf<String>()

        attachments.filter { it.kind == ChatMessageAttachmentKind.FILE }.forEach { attachment ->
            val content = attachment.fileTextContent?.trim().orEmpty()
            if (content.isNotEmpty()) {
                sections += "[Attached file: ${attachment.filename}]\n$content"
            }
        }

        val trimmedVisible = visibleText.trim()
        val speechTranscripts = attachments
            .mapNotNull { it.speechTranscript?.trim() }
            .filter { it.isNotEmpty() }

        for (transcript in speechTranscripts) {
            if (sectionExists(transcript, sections)) continue
            if (trimmedVisible.isEmpty() || !trimmedVisible.contains(transcript, ignoreCase = true)) {
                sections += transcript
            }
        }

        if (trimmedVisible.isNotEmpty()) {
            sections += trimmedVisible
        }

        return sections
    }

    private fun sectionExists(candidate: String, sections: List<String>): Boolean =
        sections.any { it.equals(candidate, ignoreCase = true) }

    fun attachmentKind(filename: String, mimeType: String?) =
        ChatAttachmentKindResolver.attachmentKind(filename, mimeType)
}
