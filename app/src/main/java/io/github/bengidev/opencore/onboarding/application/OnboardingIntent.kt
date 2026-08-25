package io.github.bengidev.opencore.onboarding.application

internal sealed interface OnboardingIntent {
    data object OnAppear : OnboardingIntent
    data class CompletionLoaded(val completed: Boolean) : OnboardingIntent
    data object FinishRequested : OnboardingIntent
    data object CompletionSaved : OnboardingIntent
}
