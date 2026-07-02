package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechComposerDraftMergerTest {
    @Test
    fun mergedDraftSpacing() {
        assertEquals("Hi there", SpeechComposerDraftMerger.merged("Hi", "there"))
        assertEquals("Hi there", SpeechComposerDraftMerger.merged("Hi ", "there"))
        assertEquals("there", SpeechComposerDraftMerger.merged("", "there"))
    }
}
