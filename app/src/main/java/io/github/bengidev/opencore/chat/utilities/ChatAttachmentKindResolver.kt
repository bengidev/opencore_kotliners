package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind

internal enum class ChatAttachmentMediaCategory {
    PLAIN_TEXT,
    IMAGE,
    VIDEO,
    UNSUPPORTED,
}

/** Resolves filenames and MIME types into chat attachment kinds. */
internal object ChatAttachmentKindResolver {
    private val plainTextExtensions = setOf(
        "txt", "md", "markdown", "json", "swift", "js", "ts", "py", "rb", "go",
        "rs", "java", "kt", "c", "cc", "cpp", "h", "hpp", "m", "mm", "sh",
        "yaml", "yml", "xml", "html", "css", "csv", "log",
    )

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "heic", "heif", "gif", "webp", "bmp", "tiff", "tif",
    )

    private val videoExtensions = setOf(
        "mp4", "mov", "m4v", "avi", "mkv", "webm",
    )

    fun attachmentKind(filename: String, mimeType: String?): ChatMessageAttachmentKind {
        mimeType?.let { mime ->
            when {
                mime.startsWith("image/") -> return ChatMessageAttachmentKind.IMAGE
                mime.startsWith("video/") -> return ChatMessageAttachmentKind.VIDEO
                mime.startsWith("audio/") -> return ChatMessageAttachmentKind.AUDIO
                mime.startsWith("text/") -> return ChatMessageAttachmentKind.FILE
            }
        }

        return when (resolve(pathExtension = filename.substringAfterLast('.', ""))) {
            ChatAttachmentMediaCategory.PLAIN_TEXT -> ChatMessageAttachmentKind.FILE
            ChatAttachmentMediaCategory.IMAGE -> ChatMessageAttachmentKind.IMAGE
            ChatAttachmentMediaCategory.VIDEO -> ChatMessageAttachmentKind.VIDEO
            ChatAttachmentMediaCategory.UNSUPPORTED -> ChatMessageAttachmentKind.FILE
        }
    }

    fun resolve(pathExtension: String): ChatAttachmentMediaCategory {
        val normalized = pathExtension.trim().lowercase()
        if (normalized.isEmpty()) return ChatAttachmentMediaCategory.UNSUPPORTED
        if (plainTextExtensions.contains(normalized)) return ChatAttachmentMediaCategory.PLAIN_TEXT
        if (imageExtensions.contains(normalized)) return ChatAttachmentMediaCategory.IMAGE
        if (videoExtensions.contains(normalized)) return ChatAttachmentMediaCategory.VIDEO
        return ChatAttachmentMediaCategory.UNSUPPORTED
    }
}
