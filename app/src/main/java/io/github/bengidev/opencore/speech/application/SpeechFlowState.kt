package io.github.bengidev.opencore.speech.application

internal data class SpeechFlowState(
    val isListening: Boolean = false,
    val isTranscribing: Boolean = false,
    /** Frozen waveform shown while post-stop transcription runs. */
    val transcribingWaveformSamples: List<Float> = emptyList(),
    val transcribingDurationSeconds: Double = 0.0,
    val partialTranscript: String = "",
    val errorMessage: String? = null,
    val elapsedDurationSeconds: Double = 0.0,
    val audioLevels: List<Float> = emptyList(),
    val isVoiceActive: Boolean = false,
)
