package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecordingLimitsTest {
    @Test
    fun autoStopThresholdIsSlightlyBelowMaxDuration() {
        assertEquals(119.75, SpeechRecordingLimits.autoStopThresholdSeconds, 0.001)
    }

    @Test
    fun shouldAutoStopAtThreshold() {
        assertFalse(SpeechRecordingLimits.shouldAutoStop(elapsedSeconds = 119.0))
        assertTrue(SpeechRecordingLimits.shouldAutoStop(elapsedSeconds = 119.75))
        assertTrue(SpeechRecordingLimits.shouldAutoStop(elapsedSeconds = 120.0))
    }
}
