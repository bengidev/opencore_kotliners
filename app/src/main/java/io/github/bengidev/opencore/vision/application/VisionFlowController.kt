package io.github.bengidev.opencore.vision.application

import android.content.Context
import android.net.Uri
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentSizeLimits
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentStore
import io.github.bengidev.opencore.chat.utilities.ChatModelInputBuilder
import io.github.bengidev.opencore.chat.utilities.ChatPlainTextFileReader
import io.github.bengidev.opencore.vision.domain.VisionMediaKind
import io.github.bengidev.opencore.vision.utilities.VisionAttachmentThumbnailLogic
import io.github.bengidev.opencore.vision.utilities.VisionProcessingDisplayLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/** Owns composer media attachment intake — copies files into durable storage for send. */
internal class VisionFlowController(
    private val context: Context,
    private val thumbnailBuilder: (ByteArray) -> ByteArray? = VisionAttachmentThumbnailLogic::jpegThumbnail,
) {
    private val _state = MutableStateFlow(VisionFlowState())
    val state: StateFlow<VisionFlowState> = _state.asStateFlow()

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun dismissProcessingPresentation() {
        _state.update { it.copy(isProcessing = false, statusMessage = null) }
    }

    suspend fun attachUri(uri: Uri, filename: String, mimeType: String?): ChatMessageAttachment? {
        clearError()
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val data = stream.readBytes()
                attachImportedData(data, filename, mimeType)
            }
        }.getOrElse { error ->
            _state.update { it.copy(errorMessage = error.message) }
            null
        }
    }

    suspend fun attachFile(file: File, mimeType: String? = null): ChatMessageAttachment? {
        clearError()
        return runCatching {
            val data = file.readBytes()
            attachImportedData(data, file.name, mimeType)
        }.getOrElse { error ->
            _state.update { it.copy(errorMessage = error.message) }
            null
        }
    }

    suspend fun attachImportedData(
        data: ByteArray,
        filename: String,
        mimeType: String?,
    ): ChatMessageAttachment? {
        clearError()
        val kind = ChatModelInputBuilder.attachmentKind(filename, mimeType)
        _state.update {
            it.copy(
                isProcessing = true,
                statusMessage = VisionProcessingDisplayLogic.statusMessage(visionKind(kind)),
            )
        }
        return try {
            ChatAttachmentSizeLimits.validateImportSize(data.size)
            if (kind == ChatMessageAttachmentKind.VIDEO) {
                ChatAttachmentSizeLimits.validateVideoWireSize(data.size)
            }

            val fileTextContent = if (kind == ChatMessageAttachmentKind.FILE) {
                ChatPlainTextFileReader.read(data)
            } else {
                null
            }

            val stored = ChatAttachmentStore.save(context, data, filename)
            val thumbnail = if (kind == ChatMessageAttachmentKind.IMAGE) thumbnailBuilder(data) else null
            ChatMessageAttachment(
                kind = kind,
                filename = filename,
                localPath = stored.absolutePath,
                thumbnailJpegData = thumbnail,
                fileTextContent = fileTextContent,
            )
        } catch (error: Exception) {
            _state.update { it.copy(errorMessage = error.message) }
            null
        } finally {
            _state.update { it.copy(isProcessing = false, statusMessage = null) }
        }
    }

    private fun visionKind(kind: ChatMessageAttachmentKind): VisionMediaKind = when (kind) {
        ChatMessageAttachmentKind.IMAGE -> VisionMediaKind.IMAGE
        ChatMessageAttachmentKind.VIDEO -> VisionMediaKind.VIDEO
        ChatMessageAttachmentKind.FILE -> VisionMediaKind.PLAIN_TEXT
        ChatMessageAttachmentKind.AUDIO -> VisionMediaKind.UNSUPPORTED
    }
}
