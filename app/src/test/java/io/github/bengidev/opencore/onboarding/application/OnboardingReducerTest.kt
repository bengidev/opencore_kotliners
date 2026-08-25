package io.github.bengidev.opencore.onboarding.application

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReducerTest {

    @Test
    fun initialState_isNotFinished() {
        val state = OnboardingState()
        assertFalse(state.isFinished)
    }

    @Test
    fun finishRequested_marksFinished() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.FinishRequested
        )
        assertTrue(result.isFinished)
    }

    @Test
    fun completionLoaded_setsFinishedFlag() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.CompletionLoaded(completed = true)
        )
        assertTrue(result.isFinished)
    }
}
