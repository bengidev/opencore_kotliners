package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SpeechRecordingDisplayLogicTest {
    @Test
    fun formatsElapsedDuration() {
        assertEquals("0:00", SpeechRecordingDisplayLogic.formatElapsedDuration(0.0))
        assertEquals("0:09", SpeechRecordingDisplayLogic.formatElapsedDuration(9.0))
        assertEquals("0:59", SpeechRecordingDisplayLogic.formatElapsedDuration(59.0))
        assertEquals("1:00", SpeechRecordingDisplayLogic.formatElapsedDuration(60.0))
        assertEquals("2:05", SpeechRecordingDisplayLogic.formatElapsedDuration(125.0))
    }

    @Test
    fun voiceActivityThreshold() {
        val threshold = SpeechRecordingDisplayLogic.DEFAULT_VOICE_ACTIVITY_THRESHOLD

        assertFalse(SpeechRecordingDisplayLogic.isVoiceActive(0f, threshold))
        assertFalse(SpeechRecordingDisplayLogic.isVoiceActive(threshold, threshold))
        assertTrue(SpeechRecordingDisplayLogic.isVoiceActive(threshold + 0.001f, threshold))
        assertTrue(SpeechRecordingDisplayLogic.isVoiceActive(0.2f, threshold))
    }

    @Test
    fun idleWaveformBars() {
        val heights = SpeechRecordingDisplayLogic.waveformBarHeights(
            levels = listOf(0.001f, 0.002f, 0.003f),
            barCount = 4,
        )

        assertEquals(4, heights.size)
        assertTrue(heights.all { it in 0.12f..0.2f })
        assertTrue(heights.distinct().size > 1)
    }

    @Test
    fun activeWaveformBars() {
        val heights = SpeechRecordingDisplayLogic.waveformBarHeights(
            levels = listOf(0.05f, 0.1f, 0.2f),
            barCount = 3,
        )

        assertEquals(3, heights.size)
        assertTrue(heights[0] > 0.12f)
        assertTrue(heights[1] > heights[0])
        assertTrue(heights[2] > heights[1])
    }

    @Test
    fun playbackWaveformBarHeights_resamplesToDenserLayout() {
        val heights = SpeechRecordingDisplayLogic.playbackWaveformBarHeights(
            levels = listOf(0.05f, 0.1f, 0.2f, 0.4f),
            barCount = 8,
        )

        assertEquals(8, heights.size)
        assertTrue(heights.first() < heights.last())
    }

    @Test
    fun composerBarCountScalesWithWidth() {
        assertEquals(16, SpeechRecordingDisplayLogic.composerBarCount(forWidthDp = 48f))
        assertTrue(SpeechRecordingDisplayLogic.composerBarCount(forWidthDp = 240f) > 16)
    }

    @Test
    fun appendWaveformSample() {
        val capacity = 3
        val first = SpeechRecordingDisplayLogic.appendWaveformSample(0.1f, emptyList(), capacity)
        val second = SpeechRecordingDisplayLogic.appendWaveformSample(0.2f, first, capacity)
        val third = SpeechRecordingDisplayLogic.appendWaveformSample(0.3f, second, capacity)
        val fourth = SpeechRecordingDisplayLogic.appendWaveformSample(0.4f, third, capacity)

        assertEquals(listOf(0.2f, 0.3f, 0.4f), fourth)
    }
}

class SpeechRecognizerLocaleResolverTest {
    @Test
    fun prefersDeviceLocale() {
        val preferred = Locale.forLanguageTag("fr-FR")

        val resolved = SpeechRecognizerLocaleResolver.resolve(preferred = preferred) { locale ->
            locale.language == "fr"
        }

        assertEquals(preferred, resolved)
    }

    @Test
    fun fallsBackToLanguageOnly() {
        val preferred = Locale.forLanguageTag("fr-CA")

        val resolved = SpeechRecognizerLocaleResolver.resolve(preferred = preferred) { locale ->
            locale.toLanguageTag() == "fr"
        }

        assertEquals("fr", resolved?.toLanguageTag())
    }

    @Test
    fun usesEnglishWhenAvailable() {
        val resolved = SpeechRecognizerLocaleResolver.resolve(
            preferred = Locale.forLanguageTag("xx-YY"),
        ) { locale ->
            locale.toLanguageTag() == "en-US"
        }

        assertEquals(Locale.forLanguageTag("en-US"), resolved)
    }

    @Test
    fun returnsNullWhenNoLocaleAvailable() {
        val resolved = SpeechRecognizerLocaleResolver.resolve(
            preferred = Locale.forLanguageTag("xx-YY"),
        ) { false }

        assertEquals(null, resolved)
    }
}
