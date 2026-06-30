package io.github.bengidev.opencore.chat.utilities

/** Reads plain-text file payloads for model input. */
internal object ChatPlainTextFileReader {
    const val MAX_BYTE_COUNT = 512_000

    fun read(data: ByteArray): String {
        if (data.size > MAX_BYTE_COUNT) {
            throw ChatAttachmentError.FileTooLarge(data.size, MAX_BYTE_COUNT)
        }
        return data.toString(Charsets.UTF_8).takeIf { it.isNotEmpty() }
            ?: throw ChatAttachmentError.UnreadableFile
    }

    fun readFromFile(localPath: String): String {
        val file = java.io.File(localPath)
        val data = file.readBytes()
        return read(data)
    }
}
