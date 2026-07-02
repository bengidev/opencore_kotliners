package io.github.bengidev.opencore.speech.utilities

import java.io.File

/** Prepares captured audio for OpenAI-compatible `/audio/transcriptions` uploads. */
internal object SpeechWhisperUploadPreparer {
  private val supportedExtensions = setOf(
    "flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm",
  )

  data class PreparedUpload(
    val file: File,
    val filename: String,
    val mimeType: String,
    val shouldDeleteAfterUpload: Boolean,
  )

  fun prepareUpload(sourceFile: File): PreparedUpload {
    val extension = sourceFile.extension.lowercase()
    require(extension in supportedExtensions) { "Unsupported audio format: $extension" }
    return PreparedUpload(
      file = sourceFile,
      filename = "audio.$extension",
      mimeType = mimeTypeFor(extension),
      shouldDeleteAfterUpload = false,
    )
  }

  private fun mimeTypeFor(extension: String): String = when (extension) {
    "wav" -> "audio/wav"
    "mp3", "mpeg", "mpga" -> "audio/mpeg"
    "m4a", "mp4" -> "audio/mp4"
    "webm" -> "audio/webm"
    "ogg" -> "audio/ogg"
    "flac" -> "audio/flac"
    else -> "application/octet-stream"
  }
}
