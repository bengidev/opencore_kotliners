package io.github.bengidev.opencore.onboarding.presenter.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingChatFeedScrollPolicyTest {

    @Test
    fun shouldDeferForActiveScroll_whenUserIsDragging() {
        assertTrue(OnboardingChatFeedScrollPolicy.shouldDeferForActiveScroll(isScrollInProgress = true))
        assertFalse(OnboardingChatFeedScrollPolicy.shouldDeferForActiveScroll(isScrollInProgress = false))
    }

    @Test
    fun shouldAnimateScroll_onlyForEarlyMessages() {
        assertTrue(OnboardingChatFeedScrollPolicy.shouldAnimateScroll(messageCount = 1))
        assertTrue(OnboardingChatFeedScrollPolicy.shouldAnimateScroll(messageCount = 3))
        assertFalse(OnboardingChatFeedScrollPolicy.shouldAnimateScroll(messageCount = 4))
    }
}
