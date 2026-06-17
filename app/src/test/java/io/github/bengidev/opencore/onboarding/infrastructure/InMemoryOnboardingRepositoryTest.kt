package io.github.bengidev.opencore.onboarding.infrastructure

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryOnboardingRepositoryTest {

    @Test
    fun startsIncomplete_thenCompletes() = runTest {
        val repository = InMemoryOnboardingRepository()
        assertFalse(repository.isOnboardingCompleted())
        repository.completeOnboarding()
        assertTrue(repository.isOnboardingCompleted())
    }
}
