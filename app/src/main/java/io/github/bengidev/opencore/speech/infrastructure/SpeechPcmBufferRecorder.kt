package io.github.bengidev.opencore.speech.infrastructure

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/** Accumulates PCM bytes from [android.speech.SpeechRecognizer] into a playable WAV file. */
internal class SpeechPcmBufferRecorder(
    private val context: Context,
    private val sampleRate: Int = SAMPLE_RATE_HZ,
) {
    private var wavFile: File? = null
    private var writer: RandomAccessFile? = null
    private var pcmBytes: Int = 0

    fun start() {
        discard()
        val file = File(context.cacheDir, "voice-note-${UUID.randomUUID()}.wav")
        val randomAccess = RandomAccessFile(file, "rw")
        randomAccess.setLength(0)
        writePlaceholderHeader(randomAccess)
        writer = randomAccess
        wavFile = file
        pcmBytes = 0
    }

    fun append(buffer: ByteArray) {
        if (buffer.isEmpty()) return
        val randomAccess = writer ?: return
        randomAccess.seek(WAV_HEADER_SIZE + pcmBytes.toLong())
        randomAccess.write(buffer)
        pcmBytes += buffer.size
    }

    fun finish(): Pair<File?, Double> {
        val file = wavFile
        val randomAccess = writer
        writer = null
        wavFile = null
        if (file == null || randomAccess == null || pcmBytes == 0) {
            runCatching { randomAccess?.close() }
            file?.delete()
            return null to 0.0
        }

        return runCatching {
            randomAccess.seek(0)
            writeFinalHeader(randomAccess, pcmBytes, sampleRate)
            randomAccess.close()
            val duration = pcmBytes / (sampleRate.toDouble() * BYTES_PER_SAMPLE)
            file to duration
        }.getOrElse {
            runCatching { randomAccess.close() }
            file.delete()
            null to 0.0
        }
    }

    fun discard() {
        runCatching { writer?.close() }
        writer = null
        wavFile?.delete()
        wavFile = null
        pcmBytes = 0
    }

    private fun writePlaceholderHeader(randomAccess: RandomAccessFile) {
        randomAccess.write(ByteArray(WAV_HEADER_SIZE))
    }

    private fun writeFinalHeader(randomAccess: RandomAccessFile, dataSize: Int, sampleRate: Int) {
        val channels = 1
        val byteRate = sampleRate * channels * BYTES_PER_SAMPLE
        val blockAlign = channels * BYTES_PER_SAMPLE
        val bitsPerSample = BYTES_PER_SAMPLE * 8

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

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val BYTES_PER_SAMPLE = 2
        private const val WAV_HEADER_SIZE = 44
    }
}
