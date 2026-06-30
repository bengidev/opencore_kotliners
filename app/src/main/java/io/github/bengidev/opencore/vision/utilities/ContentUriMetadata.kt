package io.github.bengidev.opencore.vision.utilities

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

internal object ContentUriMetadata {
    data class Resolved(
        val filename: String,
        val mimeType: String?,
    )

    fun resolve(context: Context, uri: Uri): Resolved {
        val mimeType = context.contentResolver.getType(uri)
        val displayName = queryDisplayName(context, uri)?.takeIf { it.isNotBlank() }
        val filename = displayName ?: fallbackFilename(mimeType)
        return Resolved(filename = filename, mimeType = mimeType)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }

    private fun fallbackFilename(mimeType: String?): String = when {
        mimeType?.startsWith("video/") == true -> "video.mp4"
        mimeType?.startsWith("image/") == true -> "photo.jpg"
        mimeType?.startsWith("text/") == true -> "file.txt"
        mimeType == "application/json" -> "file.json"
        else -> "attachment"
    }
}
