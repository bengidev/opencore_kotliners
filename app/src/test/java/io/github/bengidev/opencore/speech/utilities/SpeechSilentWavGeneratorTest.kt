package io.github.bengidev.opencore.speech.utilities

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpeechSilentWavGeneratorTest {
    @Test
    fun createsPlayableWavForReasonableDuration() {
        val file = SpeechSilentWavGenerator.create(
            context = RuntimeEnvironment.getApplication(),
            durationSeconds = 1.0,
        )

        assertNotNull(file)
        assertTrue(file!!.length() > 44)
        assertTrue(file.name.endsWith(".wav"))
        file.delete()
    }

    @Test
    fun skipsVeryShortDurations() {
        val file = SpeechSilentWavGenerator.create(
            context = RuntimeEnvironment.getApplication(),
            durationSeconds = 0.05,
        )

        assertTrue(file == null)
    }
}
