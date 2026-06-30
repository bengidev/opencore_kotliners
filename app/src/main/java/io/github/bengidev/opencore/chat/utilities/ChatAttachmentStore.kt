package io.github.bengidev.opencore.chat.utilities

import android.content.Context
import java.io.File
import java.util.UUID

/** Copies composer attachments into durable app storage. */
internal object ChatAttachmentStore {
    fun save(context: Context, data: ByteArray, suggestedFilename: String): File {
        ChatAttachmentSizeLimits.validateImportSize(data.size)
        val directory = attachmentsDirectory(context)
        val filename = uniqueFilename(suggestedFilename)
        val destination = File(directory, filename)
        destination.writeBytes(data)
        return destination
    }

    fun save(context: Context, copyingFrom: File, suggestedFilename: String): File {
        val data = copyingFrom.readBytes()
        return save(context, data, suggestedFilename)
    }

    fun remove(localPath: String) {
        runCatching { File(localPath).delete() }
    }

    fun removeAll(localPaths: List<String>) {
        localPaths.forEach(::remove)
    }

    private fun attachmentsDirectory(context: Context): File {
        val directory = File(context.filesDir, "chat-attachments")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun uniqueFilename(suggestedFilename: String): String {
        val sanitized = suggestedFilename.trim().ifEmpty { "attachment" }
        val base = sanitized.substringBeforeLast('.')
        val ext = sanitized.substringAfterLast('.', "")
        val suffix = if (ext.isNotEmpty()) ".$ext" else ""
        return "${base}-${UUID.randomUUID()}$suffix"
    }
}
