package io.github.bengidev.opencore.onboarding.application

import io.github.bengidev.opencore.onboarding.domain.OnboardingPageType
import io.github.bengidev.opencore.onboarding.domain.OnboardingPromptOption
import io.github.bengidev.opencore.onboarding.domain.OnboardingQueueItem

/**
 * Strategy pattern: page-specific demo reset behavior when navigating.
 */
internal interface PageDemoDefaultsStrategy {
    fun apply(demoState: OnboardingState.DemoState): OnboardingState.DemoState
}

internal object NoOpDemoDefaultsStrategy : PageDemoDefaultsStrategy {
    override fun apply(demoState: OnboardingState.DemoState): OnboardingState.DemoState = demoState
}

internal object IdeaStudioDemoDefaultsStrategy : PageDemoDefaultsStrategy {
    override fun apply(demoState: OnboardingState.DemoState): OnboardingState.DemoState =
        demoState.copy(selectedPromptIndex = 0)
}

internal object ReasoningControlDemoDefaultsStrategy : PageDemoDefaultsStrategy {
    override fun apply(demoState: OnboardingState.DemoState): OnboardingState.DemoState =
        demoState.copy(reasoningLevel = 0.62)
}

/**
 * Factory method: selects the strategy for a given page type.
 */
internal object PageDemoDefaultsStrategyFactory {
    fun forPageType(type: OnboardingPageType): PageDemoDefaultsStrategy = when (type) {
        OnboardingPageType.IdeaStudio -> IdeaStudioDemoDefaultsStrategy
        OnboardingPageType.ReasoningControl -> ReasoningControlDemoDefaultsStrategy
        else -> NoOpDemoDefaultsStrategy
    }
}
