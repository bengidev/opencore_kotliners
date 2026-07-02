package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTranscriptAccumulatorTest {
    @Test
    fun displayTranscript_returnsCurrentSegmentWhenCommittedEmpty() {
        assertEquals("hello", SpeechTranscriptAccumulator.displayTranscript("", "hello"))
    }

    @Test
    fun displayTranscript_joinsCommittedAndCurrentSegment() {
        assertEquals(
            "hello world",
            SpeechTranscriptAccumulator.displayTranscript("hello", "world"),
        )
    }

    @Test
    fun displayTranscript_usesSegmentWhenItAlreadyContainsCommittedPrefix() {
        assertEquals(
            "hello world",
            SpeechTranscriptAccumulator.displayTranscript("hello", "hello world"),
        )
    }

    @Test
    fun commitSegment_appendsFinalizedSegment() {
        assertEquals(
            "hello world",
            SpeechTranscriptAccumulator.commitSegment("hello", "world"),
        )
    }
}
