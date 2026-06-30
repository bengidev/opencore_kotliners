package io.github.bengidev.opencore.speech.utilities

import android.speech.SpeechRecognizer

/** Pure rules for on-device vs server speech recognition fallback. */
internal object SpeechRecognitionFallbackLogic {
    fun shouldRetryWithServerRecognition(
        errorMessage: String,
        attemptedOnDevice: Boolean,
        errorCode: Int? = null,
    ): Boolean {
        if (!attemptedOnDevice) return false
        if (errorCode != null && shouldRetryOnlineAfterOnDeviceError(errorCode)) return true

        val lowered = errorMessage.lowercase()
        return lowered.contains("initialize") ||
            lowered.contains("on-device") ||
            lowered.contains("on device") ||
            lowered.contains("not downloaded") ||
            lowered.contains("not available") ||
            lowered.contains("offline") ||
            lowered.contains("language pack")
    }

    /** Android often reports offline/unavailable failures as generic client or server error codes. */
    fun shouldRetryOnlineAfterOnDeviceError(errorCode: Int): Boolean {
        return when (errorCode) {
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            -> true
            else -> false
        }
    }

    fun userFacingErrorMessage(
        systemMessage: String,
        attemptedOnDevice: Boolean = false,
    ): String {
        val lowered = systemMessage.lowercase()

        if (lowered.contains("permission") || lowered.contains("authorized") || lowered.contains("denied")) {
            return "Microphone access is required for voice input."
        }

        if (lowered.contains("locale") || lowered.contains("language")) {
            return "Speech recognition is not available for your language on this device."
        }

        if (shouldRetryWithServerRecognition(systemMessage, attemptedOnDevice)) {
            return "Speech recognition could not be started. Check your network connection."
        }

        return systemMessage
    }
}
