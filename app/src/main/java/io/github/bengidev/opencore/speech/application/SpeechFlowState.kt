package io.github.bengidev.opencore.speech.application

internal data class SpeechFlowState(
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val errorMessage: String? = null,
    val elapsedDurationSeconds: Double = 0.0,
    val audioLevels: List<Float> = emptyList(),
    val isVoiceActive: Boolean = false,
)
