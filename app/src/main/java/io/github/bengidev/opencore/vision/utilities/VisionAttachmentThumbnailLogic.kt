package io.github.bengidev.opencore.vision.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.min

internal object VisionAttachmentThumbnailLogic {
    private const val MAX_DIMENSION = 240

    fun jpegThumbnail(imageData: ByteArray): ByteArray? {
        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options) ?: return null
        val longest = maxOf(bitmap.width, bitmap.height).toFloat()
        val scale = min(1f, MAX_DIMENSION / longest)
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (targetWidth != bitmap.width || targetHeight != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            if (scaled !== bitmap) scaled.recycle()
            stream.toByteArray()
        }
    }
}
