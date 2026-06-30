package io.github.bengidev.opencore.speech.utilities

import kotlin.math.floor
import kotlin.math.sqrt

/** Pure display rules for the speech recording indicator — timer, waveform, and voice activity. */
internal object SpeechRecordingDisplayLogic {
    const val DEFAULT_VOICE_ACTIVITY_THRESHOLD: Float = 0.015f
    const val WAVEFORM_SAMPLE_CAPACITY: Int = 24
    const val DEFAULT_BAR_COUNT: Int = 16
    const val PLAYBACK_BAR_COUNT: Int = 32

    private const val IDLE_BAR_HEIGHT: Float = 0.12f
    private const val MAX_BAR_HEIGHT: Float = 1.0f

    fun formatElapsedDuration(durationSeconds: Double): String {
        val totalSeconds = maxOf(0, floor(durationSeconds).toInt())
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    fun isVoiceActive(
        level: Float,
        threshold: Float = DEFAULT_VOICE_ACTIVITY_THRESHOLD,
    ): Boolean = level > threshold

    fun normalizedBarHeight(
        level: Float,
        idleHeight: Float = IDLE_BAR_HEIGHT,
        maxHeight: Float = MAX_BAR_HEIGHT,
    ): Float {
        val curved = sqrt(level.coerceIn(0f, 1f).toDouble()).toFloat()
        return (idleHeight + curved * (maxHeight - idleHeight)).coerceIn(idleHeight, maxHeight)
    }

    fun waveformBarHeights(
        levels: List<Float>,
        barCount: Int = DEFAULT_BAR_COUNT,
    ): List<Float> {
        if (barCount <= 0) return emptyList()
        if (levels.isEmpty()) {
            return List(barCount) { IDLE_BAR_HEIGHT }
        }

        var samples = levels.takeLast(barCount).toMutableList()
        while (samples.size < barCount) {
            samples.add(0, 0f)
        }
        if (samples.size > barCount) {
            samples = samples.takeLast(barCount).toMutableList()
        }

        return samples.map { level -> normalizedBarHeight(level = level) }
    }

    /** Interpolates stored samples into a denser bar layout for in-chat playback. */
    fun playbackWaveformBarHeights(
        levels: List<Float>,
        barCount: Int = PLAYBACK_BAR_COUNT,
    ): List<Float> {
        if (barCount <= 0) return emptyList()
        if (levels.isEmpty()) {
            return List(barCount) { IDLE_BAR_HEIGHT }
        }
        if (levels.size == 1) {
            val height = normalizedBarHeight(levels.first())
            return List(barCount) { height }
        }

        return List(barCount) { index ->
            val position = (index + 0.5) / barCount.toDouble() * (levels.size - 1)
            val lowerIndex = position.toInt().coerceIn(0, levels.lastIndex)
            val upperIndex = (lowerIndex + 1).coerceAtMost(levels.lastIndex)
            val fraction = (position - lowerIndex).toFloat()
            val interpolated = levels[lowerIndex] * (1f - fraction) + levels[upperIndex] * fraction
            normalizedBarHeight(interpolated)
        }
    }

    fun appendWaveformSample(
        level: Float,
        levels: List<Float>,
        capacity: Int,
    ): List<Float> {
        val updated = levels.toMutableList()
        updated.add(level)
        if (updated.size > capacity) {
            return updated.takeLast(capacity)
        }
        return updated
    }
}
