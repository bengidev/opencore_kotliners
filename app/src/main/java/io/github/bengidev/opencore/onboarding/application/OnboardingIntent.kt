package io.github.bengidev.opencore.onboarding.application

/**
 * Command pattern: each user/system action is an explicit intent object.
 * Mirrors the Action enum from OpenCore designOnboardingFeature.
 */
internal sealed interface OnboardingIntent {
    data object OnAppear : OnboardingIntent
    data class CompletionLoaded(val completed: Boolean) : OnboardingIntent
    data object NextButtonTapped : OnboardingIntent
    data object PreviousButtonTapped : OnboardingIntent
    data class PageSelected(val index: Int) : OnboardingIntent
    data object SkipButtonTapped : OnboardingIntent
    data object FinishButtonTapped : OnboardingIntent
    data object CompletionSaved : OnboardingIntent
    data class PromptChipTapped(val index: Int) : OnboardingIntent
    data object AddQueuedPromptButtonTapped : OnboardingIntent
    data class ReasoningLevelChanged(val value: Double) : OnboardingIntent
    data object PairingToggleTapped : OnboardingIntent
}
