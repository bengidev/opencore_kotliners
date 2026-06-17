package io.github.bengidev.opencore.onboarding.domain

internal enum class OnboardingPageType {
    EncryptedPairing,
    IdeaStudio,
    PromptQueue,
    ReasoningControl,
    WorkspaceReady;

    companion object {
        val allCases: List<OnboardingPageType> = entries
    }
}
