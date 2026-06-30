package io.github.bengidev.opencore.speech.domain

internal sealed interface SpeechRecognitionEvent {
    data object Ready : SpeechRecognitionEvent
    data class Partial(val text: String) : SpeechRecognitionEvent
    data class Final(val text: String) : SpeechRecognitionEvent
    data class Failed(val message: String) : SpeechRecognitionEvent
    data class AudioLevel(val level: Float) : SpeechRecognitionEvent
}
