package io.github.bengidev.opencore.onboarding.application

internal object OnboardingReducer {

    internal fun reduce(state: OnboardingState, intent: OnboardingIntent): OnboardingState = when (intent) {
        is OnboardingIntent.OnAppear,
        is OnboardingIntent.CompletionSaved -> state

        is OnboardingIntent.CompletionLoaded -> state.copy(isFinished = intent.completed)

        is OnboardingIntent.FinishRequested -> state.copy(isFinished = true)
    }
}
