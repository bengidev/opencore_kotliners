package io.github.bengidev.opencore.onboarding.application

import io.github.bengidev.opencore.onboarding.domain.OnboardingPromptOption
import io.github.bengidev.opencore.onboarding.domain.OnboardingQueueItem

/**
 * Pure reducer that applies [OnboardingIntent] commands to [OnboardingState].
 * OpenCoreOnboardingFeature reducer body.
 */
internal object OnboardingReducer {

    internal fun reduce(state: OnboardingState, intent: OnboardingIntent): OnboardingState = when (intent) {
        is OnboardingIntent.OnAppear,
        is OnboardingIntent.CompletionSaved -> state

        is OnboardingIntent.CompletionLoaded -> state.copy(isFinished = intent.completed)

        is OnboardingIntent.NextButtonTapped -> {
            val nextPage = (state.currentPage + 1).coerceAtMost(state.totalPages - 1)
            state.withPageChange(nextPage)
        }

        is OnboardingIntent.PreviousButtonTapped -> {
            val previousPage = (state.currentPage - 1).coerceAtLeast(0)
            state.withPageChange(previousPage)
        }

        is OnboardingIntent.PageSelected -> {
            val selectedPage = intent.index.coerceIn(0, state.totalPages - 1)
            state.withPageChange(selectedPage)
        }

        is OnboardingIntent.SkipButtonTapped -> state.copy(currentPage = state.totalPages - 1)

        is OnboardingIntent.FinishButtonTapped -> state.copy(isFinished = true)

        is OnboardingIntent.PromptChipTapped -> {
            val safeIndex = intent.index.coerceIn(0, OnboardingPromptOption.samples.size - 1)
            state.copy(demoState = state.demoState.copy(selectedPromptIndex = safeIndex))
        }

        is OnboardingIntent.AddQueuedPromptButtonTapped -> {
            val newCount = if (state.demoState.queuedPromptCount >= OnboardingQueueItem.samples.size) {
                2
            } else {
                state.demoState.queuedPromptCount + 1
            }
            state.copy(demoState = state.demoState.copy(queuedPromptCount = newCount))
        }

        is OnboardingIntent.ReasoningLevelChanged -> {
            val clamped = intent.value.coerceIn(0.0, 1.0)
            state.copy(demoState = state.demoState.copy(reasoningLevel = clamped))
        }

        is OnboardingIntent.PairingToggleTapped ->
            state.copy(demoState = state.demoState.copy(pairingConfirmed = !state.demoState.pairingConfirmed))
    }

    private fun OnboardingState.withPageChange(newPage: Int): OnboardingState {
        if (newPage == currentPage) return copy(currentPage = newPage)
        val pageType = pages.getOrNull(newPage)?.type ?: return copy(currentPage = newPage)
        val strategy = PageDemoDefaultsStrategyFactory.forPageType(pageType)
        return copy(currentPage = newPage, demoState = strategy.apply(demoState))
    }
}
