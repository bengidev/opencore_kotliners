package io.github.bengidev.opencore.speech.utilities

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/** Creates a short silent WAV when recognizer PCM buffers are unavailable on the device. */
internal object SpeechSilentWavGenerator {
    private const val SAMPLE_RATE_HZ = 16_000
    private const val BYTES_PER_SAMPLE = 2
    private const val WAV_HEADER_SIZE = 44
    private const val MIN_DURATION_SECONDS = 0.2
    private const val MAX_DURATION_SECONDS = 120.0

    fun create(context: Context, durationSeconds: Double): File? {
        if (durationSeconds < MIN_DURATION_SECONDS) return null
        val boundedDuration = durationSeconds.coerceAtMost(MAX_DURATION_SECONDS)
        val sampleCount = (boundedDuration * SAMPLE_RATE_HZ).toLong().coerceAtLeast(1)
        val dataSize = (sampleCount * BYTES_PER_SAMPLE).toInt()
        val file = File(context.cacheDir, "voice-note-silent-${UUID.randomUUID()}.wav")

        return runCatching {
            RandomAccessFile(file, "rw").use { randomAccess ->
                randomAccess.setLength((WAV_HEADER_SIZE + dataSize).toLong())
                writeHeader(randomAccess, dataSize, SAMPLE_RATE_HZ)
            }
            file
        }.getOrNull()
    }

    private fun writeHeader(randomAccess: RandomAccessFile, dataSize: Int, sampleRate: Int) {
        val channels = 1
        val byteRate = sampleRate * channels * BYTES_PER_SAMPLE
        val blockAlign = channels * BYTES_PER_SAMPLE
        val bitsPerSample = BYTES_PER_SAMPLE * 8

        randomAccess.seek(0)
        randomAccess.write("RIFF".toByteArray())
        randomAccess.writeIntLe(36 + dataSize)
        randomAccess.write("WAVE".toByteArray())
        randomAccess.write("fmt ".toByteArray())
        randomAccess.writeIntLe(16)
        randomAccess.writeShortLe(1)
        randomAccess.writeShortLe(channels)
        randomAccess.writeIntLe(sampleRate)
        randomAccess.writeIntLe(byteRate)
        randomAccess.writeShortLe(blockAlign)
        randomAccess.writeShortLe(bitsPerSample)
        randomAccess.write("data".toByteArray())
        randomAccess.writeIntLe(dataSize)
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                (value shr 8 and 0xFF).toByte(),
                (value shr 16 and 0xFF).toByte(),
                (value shr 24 and 0xFF).toByte(),
            ),
        )
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                (value shr 8 and 0xFF).toByte(),
            ),
        )
    }
}
