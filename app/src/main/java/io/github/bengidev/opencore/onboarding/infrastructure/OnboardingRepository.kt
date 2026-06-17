package io.github.bengidev.opencore.onboarding.infrastructure

internal interface OnboardingRepository {
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun completeOnboarding()
}
