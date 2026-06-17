package io.github.bengidev.opencore.onboarding.application

import io.github.bengidev.opencore.onboarding.domain.OnboardingDemoDefaults
import io.github.bengidev.opencore.onboarding.domain.OnboardingPage
import io.github.bengidev.opencore.onboarding.domain.OnboardingPromptOption
import io.github.bengidev.opencore.onboarding.domain.OnboardingQueueItem

internal data class OnboardingState(
    val currentPage: Int = 0,
    val isFinished: Boolean = false,
    val demoState: DemoState = DemoState()
) {
    val pages: List<OnboardingPage> = OnboardingPage.all
    val totalPages: Int get() = pages.size
    val isLastPage: Boolean get() = currentPage >= totalPages - 1
    val currentPageData: OnboardingPage
        get() = pages[currentPage.coerceIn(0, totalPages - 1)]

    internal data class DemoState(
        val selectedPromptIndex: Int = OnboardingDemoDefaults.selectedPromptIndex,
        val queuedPromptCount: Int = OnboardingDemoDefaults.queuedPromptCount,
        val reasoningLevel: Double = OnboardingDemoDefaults.reasoningLevel,
        val pairingConfirmed: Boolean = OnboardingDemoDefaults.pairingConfirmed,
        val promptOptions: List<OnboardingPromptOption> = OnboardingPromptOption.samples,
        val queueItems: List<OnboardingQueueItem> = OnboardingQueueItem.samples
    )
}
