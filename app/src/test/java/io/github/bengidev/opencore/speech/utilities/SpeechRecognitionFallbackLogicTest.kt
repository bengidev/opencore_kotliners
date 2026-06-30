package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionFallbackLogicTest {
    @Test
    fun retriesAfterOnDeviceInitializationFailure() {
        assertTrue(
            SpeechRecognitionFallbackLogic.shouldRetryWithServerRecognition(
                errorMessage = "Failed to initialize recognizer",
                attemptedOnDevice = true,
            ),
        )
        assertTrue(
            SpeechRecognitionFallbackLogic.shouldRetryWithServerRecognition(
                errorMessage = "On-device recognition is not available",
                attemptedOnDevice = true,
            ),
        )
    }

    @Test
    fun retriesAfterOnDeviceClientError() {
        assertTrue(
            SpeechRecognitionFallbackLogic.shouldRetryWithServerRecognition(
                errorMessage = "Speech recognition client error.",
                attemptedOnDevice = true,
                errorCode = 5, // SpeechRecognizer.ERROR_CLIENT
            ),
        )
    }

    @Test
    fun doesNotRetryAfterServerFailure() {
        assertFalse(
            SpeechRecognitionFallbackLogic.shouldRetryWithServerRecognition(
                errorMessage = "Failed to initialize recognizer",
                attemptedOnDevice = false,
            ),
        )
    }

    @Test
    fun mapsInitializationErrors() {
        val message = SpeechRecognitionFallbackLogic.userFacingErrorMessage(
            systemMessage = "Failed to initialize recognizer",
            attemptedOnDevice = true,
        )

        assertTrue(message.contains("network", ignoreCase = true))
    }
}
