package io.github.bengidev.opencore.chat.utilities

import android.util.Base64
import java.io.File

/** Builds OpenRouter-compatible base64 video data URLs from local files. */
internal object ChatMultimodalVideoPayloadLogic {
    fun dataUrlFromFile(localPath: String): String? {
        val file = File(localPath)
        if (!file.exists() || file.length() == 0L) return null
        val data = file.readBytes()
        val mimeType = mimeTypeForFilename(file.name)
        val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
        return "data:$mimeType;base64,$encoded"
    }

    fun mimeTypeForFilename(filename: String): String = when (filename.substringAfterLast('.').lowercase()) {
        "mov" -> "video/quicktime"
        "mpeg", "mpg" -> "video/mpeg"
        "webm" -> "video/webm"
        else -> "video/mp4"
    }
}
