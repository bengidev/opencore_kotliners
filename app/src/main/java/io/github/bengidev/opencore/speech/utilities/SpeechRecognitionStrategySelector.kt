package io.github.bengidev.opencore.speech.utilities

/** Picks capture+Whisper vs on-device system recognition for a session. */
internal class SpeechRecognitionStrategySelector(
    private val captureClient: SpeechRecognitionClient,
    private val systemClient: SpeechRecognitionClient,
    private val remoteTranscriptionAvailable: suspend () -> Boolean,
) {
    suspend fun select(): SpeechRecognitionClient =
        if (remoteTranscriptionAvailable()) captureClient else systemClient
}
