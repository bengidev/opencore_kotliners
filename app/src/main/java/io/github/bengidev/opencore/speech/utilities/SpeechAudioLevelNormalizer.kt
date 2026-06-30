package io.github.bengidev.opencore.speech.utilities

import kotlin.math.sqrt

/** Maps Android speech-recognition audio signals to a 0..1 display level. */
internal object SpeechAudioLevelNormalizer {
  private const val RMS_QUIET_FLOOR = -2f
  private const val RMS_LOUD_CEILING = 10f
  private const val PCM_REFERENCE_AMPLITUDE = 4096.0
  private const val MEDIA_RECORDER_LOUD_REFERENCE = 12_000.0

  fun fromRmsDecibels(rmsdB: Float): Float {
    val span = RMS_LOUD_CEILING - RMS_QUIET_FLOOR
    if (span <= 0f) return 0f
    return ((rmsdB - RMS_QUIET_FLOOR) / span).coerceIn(0f, 1f)
  }

  fun fromMediaRecorderAmplitude(amplitude: Int): Float {
    if (amplitude <= 0) return 0f
    val linear = (amplitude / MEDIA_RECORDER_LOUD_REFERENCE).coerceIn(0.0, 1.0)
    return sqrt(linear).toFloat()
  }

  fun fromPcmBuffer(buffer: ByteArray): Float {
    if (buffer.size < 2) return 0f

    var sumSquares = 0.0
    var samples = 0
    var index = 0
    while (index + 1 < buffer.size) {
      val sample = (buffer[index].toInt() shl 8) or (buffer[index + 1].toInt() and 0xFF)
      sumSquares += sample.toDouble() * sample.toDouble()
      samples++
      index += 2
    }
    if (samples == 0) return 0f

    val rms = sqrt(sumSquares / samples)
    return (rms / PCM_REFERENCE_AMPLITUDE).toFloat().coerceIn(0f, 1f)
  }
}
