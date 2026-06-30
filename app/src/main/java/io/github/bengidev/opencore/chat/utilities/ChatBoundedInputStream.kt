package io.github.bengidev.opencore.chat.utilities

import java.io.InputStream

internal fun InputStream.readBytesUpTo(maxBytes: Int): ByteArray {
    val buffer = ByteArray(8_192)
    val output = java.io.ByteArrayOutputStream()
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read <= 0) break
        if (total + read > maxBytes) {
            throw ChatAttachmentError.ImportTooLarge(total + read, maxBytes)
        }
        output.write(buffer, 0, read)
        total += read
    }
    return output.toByteArray()
}
