package io.github.bengidev.opencore.onboarding.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.onboarding.infrastructure.InMemoryOnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenRepositoryReportsCompleted_onCompleteIsInvoked() = runTest(testDispatcher) {
        var didComplete = false
        val lifecycle = LifecycleRegistry().apply { resume() }

        OnboardingComponent(
            componentContext = DefaultComponentContext(lifecycle),
            repository = InMemoryOnboardingRepository(completed = true),
            onComplete = { didComplete = true }
        )

        advanceUntilIdle()
        assertTrue(didComplete)
    }

    @Test
    fun whenRepositoryReportsIncomplete_onCompleteIsNotInvoked() = runTest(testDispatcher) {
        var didComplete = false
        val lifecycle = LifecycleRegistry().apply { resume() }

        OnboardingComponent(
            componentContext = DefaultComponentContext(lifecycle),
            repository = InMemoryOnboardingRepository(completed = false),
            onComplete = { didComplete = true }
        )

        advanceUntilIdle()
        assertFalse(didComplete)
    }

    @Test
    fun finishButtonTapped_persistsCompletionAndInvokesOnComplete() = runTest(testDispatcher) {
        val repository = InMemoryOnboardingRepository()
        var didComplete = false
        val lifecycle = LifecycleRegistry().apply { resume() }

        val component = OnboardingComponent(
            componentContext = DefaultComponentContext(lifecycle),
            repository = repository,
            onComplete = { didComplete = true }
        )

        advanceUntilIdle()

        component.onFinishTapped()
        advanceUntilIdle()

        assertTrue(didComplete)
        assertTrue(repository.isOnboardingCompleted())
    }
}
