package io.github.bengidev.opencore.onboarding.presenter.chat

internal object OnboardingChatFeedScrollPolicy {
    fun shouldAnimateScroll(messageCount: Int): Boolean = messageCount <= 3

    /** Defer auto-scroll while the user is actively dragging the feed. */
    fun shouldDeferForActiveScroll(isScrollInProgress: Boolean): Boolean = isScrollInProgress
}
