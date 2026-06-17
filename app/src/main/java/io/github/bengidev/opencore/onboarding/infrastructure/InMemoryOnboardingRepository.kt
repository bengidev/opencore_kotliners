package io.github.bengidev.opencore.onboarding.infrastructure

/**
 * In-memory repository for unit tests and previews.
 */
internal class InMemoryOnboardingRepository(
    private var completed: Boolean = false
) : OnboardingRepository {

    override suspend fun isOnboardingCompleted(): Boolean = completed

    override suspend fun completeOnboarding() {
        completed = true
    }
}
