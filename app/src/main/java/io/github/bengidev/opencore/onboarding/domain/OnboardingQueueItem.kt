package io.github.bengidev.opencore.onboarding.domain

internal data class OnboardingQueueItem(
    val title: String,
    val detail: String,
    val status: Status
) {
    val id: String get() = title

    internal enum class Status {
        RUNNING,
        NEXT,
        QUEUED,
        READY
    }

    companion object {
        val samples: List<OnboardingQueueItem> = listOf(
            OnboardingQueueItem(
                title = "Map onboarding state",
                detail = "Engine already owns current page",
                status = Status.RUNNING
            ),
            OnboardingQueueItem(
                title = "Generate interface cards",
                detail = "No vertical scroll, compact content",
                status = Status.NEXT
            ),
            OnboardingQueueItem(
                title = "Persist completion",
                detail = "Storage writes local progress",
                status = Status.QUEUED
            ),
            OnboardingQueueItem(
                title = "Review model budget",
                detail = "Reasoning slider updates the run",
                status = Status.READY
            )
        )
    }
}
