package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVoiceNotePlaybackDisplayLogicTest {
    @Test
    fun playbackProgress_clampsToUnitInterval() {
        assertEquals(0.0, ChatVoiceNotePlaybackDisplayLogic.playbackProgress(0.0, 0.0), 0.0001)
        assertEquals(0.1, ChatVoiceNotePlaybackDisplayLogic.playbackProgress(1.0, 10.0), 0.0001)
        assertEquals(0.5, ChatVoiceNotePlaybackDisplayLogic.playbackProgress(1.5, 3.0), 0.0001)
        assertEquals(1.0, ChatVoiceNotePlaybackDisplayLogic.playbackProgress(5.0, 3.0), 0.0001)
        assertEquals(0.0, ChatVoiceNotePlaybackDisplayLogic.playbackProgress(-1.0, 3.0), 0.0001)
    }

    @Test
    fun displayedDuration_showsElapsedWhileActive() {
        assertEquals(3.0, ChatVoiceNotePlaybackDisplayLogic.displayedDuration(1.2, 3.0, isPlaybackActive = false), 0.0001)
        assertEquals(1.2, ChatVoiceNotePlaybackDisplayLogic.displayedDuration(1.2, 3.0, isPlaybackActive = true), 0.0001)
    }

    @Test
    fun isBarPlayed_marksBarsUpToProgress() {
        val barCount = 4
        assertFalse(ChatVoiceNotePlaybackDisplayLogic.isBarPlayed(0, barCount, 0.0))
        assertTrue(ChatVoiceNotePlaybackDisplayLogic.isBarPlayed(0, barCount, 0.25))
        assertTrue(ChatVoiceNotePlaybackDisplayLogic.isBarPlayed(1, barCount, 0.5))
        assertFalse(ChatVoiceNotePlaybackDisplayLogic.isBarPlayed(2, barCount, 0.5))
        assertTrue(ChatVoiceNotePlaybackDisplayLogic.isBarPlayed(3, barCount, 1.0))
    }

    @Test
    fun barPlaybackFill_interpolatesWithinSegment() {
        val barCount = 4
        assertEquals(0.0, ChatVoiceNotePlaybackDisplayLogic.barPlaybackFill(0, barCount, 0.0), 0.0001)
        assertEquals(0.5, ChatVoiceNotePlaybackDisplayLogic.barPlaybackFill(0, barCount, 0.125), 0.0001)
        assertEquals(1.0, ChatVoiceNotePlaybackDisplayLogic.barPlaybackFill(0, barCount, 0.25), 0.0001)
        assertEquals(0.0, ChatVoiceNotePlaybackDisplayLogic.barPlaybackFill(1, barCount, 0.25), 0.0001)
        assertEquals(0.5, ChatVoiceNotePlaybackDisplayLogic.barPlaybackFill(1, barCount, 0.375), 0.0001)
    }
}
