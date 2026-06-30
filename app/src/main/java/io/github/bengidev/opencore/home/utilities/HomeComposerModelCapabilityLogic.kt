package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

/** Composer warnings for model capability mismatches. */
internal object HomeComposerModelCapabilityLogic {
    sealed class VisualAttachmentDecision {
        data object Allowed : VisualAttachmentDecision()
        data class Blocked(val message: String) : VisualAttachmentDecision()
    }

    fun supportsImageInput(model: SidePanelModel?): Boolean = model?.supportsImageInput == true

    fun supportsVideoInput(model: SidePanelModel?): Boolean = model?.supportsVideoInput == true

    fun hasImageAttachments(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any { it.kind == ChatMessageAttachmentKind.IMAGE }

    fun hasVideoAttachments(attachments: List<ChatMessageAttachment>): Boolean =
        attachments.any { it.kind == ChatMessageAttachmentKind.VIDEO }

    fun hasUnsupportedVisualAttachments(
        attachments: List<ChatMessageAttachment>,
        model: SidePanelModel?,
    ): Boolean {
        if (hasImageAttachments(attachments) && !supportsImageInput(model)) return true
        if (hasVideoAttachments(attachments) && !supportsVideoInput(model)) return true
        return false
    }

    fun validateDraft(
        attachments: List<ChatMessageAttachment>,
        model: SidePanelModel?,
        modelName: String,
    ): VisualAttachmentDecision {
        if (!hasUnsupportedVisualAttachments(attachments, model)) {
            return VisualAttachmentDecision.Allowed
        }
        return VisualAttachmentDecision.Blocked(
            visualInputWarningMessage(modelName, attachments),
        )
    }

    fun validateNewAttachment(
        attachment: ChatMessageAttachment,
        model: SidePanelModel?,
        modelName: String,
    ): VisualAttachmentDecision = when (attachment.kind) {
        ChatMessageAttachmentKind.IMAGE if !supportsImageInput(model) ->
            VisualAttachmentDecision.Blocked(imageInputWarningMessage(modelName))
        ChatMessageAttachmentKind.VIDEO if !supportsVideoInput(model) ->
            VisualAttachmentDecision.Blocked(
                "$modelName does not support video input. Choose a video-capable model to attach videos.",
            )
        else -> VisualAttachmentDecision.Allowed
    }

    fun visualInputWarningMessage(modelName: String, attachments: List<ChatMessageAttachment>): String {
        val hasImages = hasImageAttachments(attachments)
        val hasVideos = hasVideoAttachments(attachments)
        return when {
            hasImages && hasVideos ->
                "$modelName does not support the attached photos and videos. Choose a vision-capable model before sending."
            hasImages -> imageInputWarningMessage(modelName)
            hasVideos ->
                "$modelName does not support video input. Choose a video-capable model before sending."
            else -> "$modelName does not support this attachment type."
        }
    }

    fun imageInputWarningMessage(modelName: String): String =
        "$modelName does not support image input. Choose a vision-capable model before sending."
}
