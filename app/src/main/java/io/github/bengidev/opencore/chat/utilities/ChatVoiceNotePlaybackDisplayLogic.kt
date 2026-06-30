package io.github.bengidev.opencore.chat.utilities

/** Pure display rules for in-chat voice-note playback — timer and waveform progress. */
internal object ChatVoiceNotePlaybackDisplayLogic {
    fun playbackProgress(currentTime: Double, duration: Double): Double {
        if (duration <= 0) return 0.0
        return (currentTime / duration).coerceIn(0.0, 1.0)
    }

    fun displayedDuration(currentTime: Double, totalDuration: Double, isPlaybackActive: Boolean): Double =
        if (isPlaybackActive) currentTime else totalDuration

    fun isBarPlayed(barIndex: Int, barCount: Int, progress: Double): Boolean =
        barPlaybackFill(barIndex, barCount, progress) >= 1.0

    /** 0..1 fill within a single bar segment — enables smooth partial highlighting. */
    fun barPlaybackFill(barIndex: Int, barCount: Int, progress: Double): Double {
        if (barCount <= 0 || barIndex < 0 || barIndex >= barCount) return 0.0
        val segmentStart = barIndex.toDouble() / barCount.toDouble()
        val segmentEnd = (barIndex + 1).toDouble() / barCount.toDouble()
        val segmentWidth = segmentEnd - segmentStart
        if (segmentWidth <= 0.0) return 0.0
        return ((progress - segmentStart) / segmentWidth).coerceIn(0.0, 1.0)
    }
}
