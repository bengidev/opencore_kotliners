package io.github.bengidev.opencore.onboarding.application

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from openzone-swifters OnboardingFeatureTests.
 * Exercises the Command pattern via [OnboardingReducer].
 */
class OnboardingReducerTest {

    @Test
    fun initialState_isPageZeroNotFinished() {
        val state = OnboardingState()
        assertEquals(0, state.currentPage)
        assertFalse(state.isFinished)
        assertEquals(5, state.totalPages)
        assertFalse(state.isLastPage)
    }

    @Test
    fun nextButtonTapped_advancesPage() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.NextButtonTapped
        )
        assertEquals(1, result.currentPage)
    }

    @Test
    fun previousButtonTapped_goesBack() {
        val result = OnboardingReducer.reduce(
            OnboardingState(currentPage = 1),
            OnboardingIntent.PreviousButtonTapped
        )
        assertEquals(0, result.currentPage)
    }

    @Test
    fun previousButtonTapped_clampsAtZero() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.PreviousButtonTapped
        )
        assertEquals(0, result.currentPage)
    }

    @Test
    fun pageSelected_jumpsToIndex() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.PageSelected(3)
        )
        assertEquals(3, result.currentPage)
    }

    @Test
    fun skipButtonTapped_jumpsToLastPage() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.SkipButtonTapped
        )
        assertEquals(4, result.currentPage)
    }

    @Test
    fun promptChipTapped_updatesSelection() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.PromptChipTapped(2)
        )
        assertEquals(2, result.demoState.selectedPromptIndex)
    }

    @Test
    fun addQueuedPromptButtonTapped_incrementsCount() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.AddQueuedPromptButtonTapped
        )
        assertEquals(3, result.demoState.queuedPromptCount)
    }

    @Test
    fun reasoningLevelChanged_clampsValue() {
        val high = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.ReasoningLevelChanged(1.5)
        )
        assertEquals(1.0, high.demoState.reasoningLevel, 0.001)

        val low = OnboardingReducer.reduce(
            high,
            OnboardingIntent.ReasoningLevelChanged(-0.5)
        )
        assertEquals(0.0, low.demoState.reasoningLevel, 0.001)
    }

    @Test
    fun pairingToggleTapped_togglesState() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.PairingToggleTapped
        )
        assertFalse(result.demoState.pairingConfirmed)
    }

    @Test
    fun finishButtonTapped_marksFinished() {
        val result = OnboardingReducer.reduce(
            OnboardingState(),
            OnboardingIntent.FinishButtonTapped
        )
        assertTrue(result.isFinished)
    }

    @Test
    fun navigatingToIdeaStudio_resetsPromptIndexViaStrategy() {
        val state = OnboardingState(
            demoState = OnboardingState.DemoState(selectedPromptIndex = 2)
        )
        val result = OnboardingReducer.reduce(state, OnboardingIntent.PageSelected(1))
        assertEquals(0, result.demoState.selectedPromptIndex)
    }

    @Test
    fun navigatingToReasoningControl_resetsLevelViaStrategy() {
        val state = OnboardingState(
            demoState = OnboardingState.DemoState(reasoningLevel = 0.1)
        )
        val result = OnboardingReducer.reduce(state, OnboardingIntent.PageSelected(3))
        assertEquals(0.62, result.demoState.reasoningLevel, 0.001)
    }
}
