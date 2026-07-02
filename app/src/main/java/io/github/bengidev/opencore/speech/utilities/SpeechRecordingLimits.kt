package io.github.bengidev.opencore.speech.utilities

/** Recording duration guardrails aligned with remodex-style voice clips. */
internal object SpeechRecordingLimits {
    /** Maximum clip length before auto-stop (seconds). */
    const val MAX_DURATION_SECONDS: Double = 120.0

    /** Stops slightly before the hard cap so validation never rejects the clip. */
    private const val AUTO_STOP_LEAD_TIME_SECONDS: Double = 0.25

    val autoStopThresholdSeconds: Double
        get() = maxOf(0.0, MAX_DURATION_SECONDS - AUTO_STOP_LEAD_TIME_SECONDS)

    fun shouldAutoStop(
        elapsedSeconds: Double,
        thresholdSeconds: Double = autoStopThresholdSeconds,
    ): Boolean = elapsedSeconds >= thresholdSeconds
}
