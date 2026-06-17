package io.github.bengidev.opencore.onboarding

import android.content.Context
import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.onboarding.application.OnboardingComponent
import io.github.bengidev.opencore.onboarding.infrastructure.DataStoreOnboardingRepository
import io.github.bengidev.opencore.onboarding.infrastructure.OnboardingRepository

/**
 * Facade pattern: single entry point for the app module to wire onboarding dependencies.
 */
internal class OnboardingFacade(
    private val repositoryFactory: (Context) -> OnboardingRepository = { DataStoreOnboardingRepository(it) }
) {
    fun createComponent(
        context: Context,
        componentContext: ComponentContext,
        onComplete: () -> Unit
    ): OnboardingComponent = OnboardingComponent(
        componentContext = componentContext,
        repository = repositoryFactory(context.applicationContext),
        onComplete = onComplete
    )
}
