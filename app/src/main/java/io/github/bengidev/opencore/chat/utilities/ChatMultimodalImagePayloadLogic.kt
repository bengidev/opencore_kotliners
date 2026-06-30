package io.github.bengidev.opencore.chat.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Normalizes local images into OpenRouter-compatible JPEG data URLs. */
internal object ChatMultimodalImagePayloadLogic {
    const val MAX_PAYLOAD_DIMENSION = 1600
    const val COMPRESSION_QUALITY = 60

    fun dataUrl(imageData: ByteArray): String? {
        val jpegData = normalizedJpegData(imageData) ?: return null
        val encoded = Base64.encodeToString(jpegData, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    fun dataUrlFromFile(localPath: String): String? {
        val file = File(localPath)
        if (!file.exists()) return null
        return dataUrl(file.readBytes())
    }

    fun normalizedJpegData(sourceData: ByteArray): ByteArray? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceData, 0, sourceData.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeByteArray(sourceData, 0, sourceData.size, decodeOptions) ?: return null

        val sourceWidth = bitmap.width.toFloat()
        val sourceHeight = bitmap.height.toFloat()
        val longestSide = max(sourceWidth, sourceHeight)
        val scale = min(1f, MAX_PAYLOAD_DIMENSION / longestSide)
        val targetWidth = floor(sourceWidth * scale).toInt().coerceAtLeast(1)
        val targetHeight = floor(sourceHeight * scale).toInt().coerceAtLeast(1)

        val scaled = if (targetWidth != bitmap.width || targetHeight != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }

        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, stream)
            if (scaled !== bitmap) scaled.recycle()
            stream.toByteArray()
        }
    }
}
