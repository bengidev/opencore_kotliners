package io.github.bengidev.opencore.chat.utilities

internal sealed class ChatAttachmentError : Exception() {
    data object UnreadableFile : ChatAttachmentError()
    data class FileTooLarge(val byteCount: Int, val limit: Int) : ChatAttachmentError()
    data class ImportTooLarge(val byteCount: Int, val limit: Int) : ChatAttachmentError()
    data class VisualEncodingFailed(val filename: String) : ChatAttachmentError()

    override val message: String?
        get() = when (this) {
            UnreadableFile -> "Could not read the selected file."
            is FileTooLarge -> {
                val sizeMb = byteCount / (1024 * 1024)
                val limitMb = limit / (1024 * 1024)
                "File is too large ($sizeMb MB). Maximum size is $limitMb MB."
            }
            is ImportTooLarge -> {
                val sizeMb = byteCount / (1024 * 1024)
                val limitMb = limit / (1024 * 1024)
                "Attachment is too large ($sizeMb MB). Maximum size is $limitMb MB."
            }
            is VisualEncodingFailed -> "Could not prepare $filename for sending."
        }
}
