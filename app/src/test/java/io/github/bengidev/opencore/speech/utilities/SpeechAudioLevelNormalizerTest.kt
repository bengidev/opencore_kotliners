package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechAudioLevelNormalizerTest {
    @Test
    fun mapsRmsDecibelsAcrossDeviceRange() {
        assertEquals(0f, SpeechAudioLevelNormalizer.fromRmsDecibels(-2f), 0.001f)
        assertEquals(0.5f, SpeechAudioLevelNormalizer.fromRmsDecibels(4f), 0.001f)
        assertEquals(1f, SpeechAudioLevelNormalizer.fromRmsDecibels(10f), 0.001f)
    }

    @Test
    fun mapsPcmBufferAmplitude() {
        val silence = ByteArray(256)
        assertEquals(0f, SpeechAudioLevelNormalizer.fromPcmBuffer(silence), 0.001f)

        val loud = ByteArray(256)
        for (index in loud.indices step 2) {
            loud[index] = 0x30
            loud[index + 1] = 0x00
        }
        val level = SpeechAudioLevelNormalizer.fromPcmBuffer(loud)
        assertTrue(level > 0.2f)
    }

    @Test
    fun mapsMediaRecorderAmplitude() {
        assertEquals(0f, SpeechAudioLevelNormalizer.fromMediaRecorderAmplitude(0), 0.001f)
        val mid = SpeechAudioLevelNormalizer.fromMediaRecorderAmplitude(3_000)
        assertTrue(mid > 0.3f)
        assertTrue(mid < 0.7f)
        assertEquals(1f, SpeechAudioLevelNormalizer.fromMediaRecorderAmplitude(20_000), 0.001f)
    }

    @Test
    fun pcmLevelIsBounded() {
        val maxed = ByteArray(256) { index ->
            if (index % 2 == 0) 0x7F.toByte() else 0xFF.toByte()
        }
        assertEquals(1f, SpeechAudioLevelNormalizer.fromPcmBuffer(maxed), 0.001f)
    }
}
