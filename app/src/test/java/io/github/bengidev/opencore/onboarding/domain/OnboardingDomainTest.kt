package io.github.bengidev.opencore.onboarding.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from openzone-swifters OnboardingTests (domain suite).
 */
class OnboardingDomainTest {

    @Test
    fun onboardingPage_all_hasFivePagesInCorrectOrder() {
        assertEquals(5, OnboardingPage.all.size)
        assertEquals(OnboardingPageType.EncryptedPairing, OnboardingPage.all[0].type)
        assertEquals(OnboardingPageType.IdeaStudio, OnboardingPage.all[1].type)
        assertEquals(OnboardingPageType.PromptQueue, OnboardingPage.all[2].type)
        assertEquals(OnboardingPageType.ReasoningControl, OnboardingPage.all[3].type)
        assertEquals(OnboardingPageType.WorkspaceReady, OnboardingPage.all[4].type)
    }

    @Test
    fun onboardingPage_hasUniqueIds() {
        val ids = OnboardingPage.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun onboardingPromptOption_hasThreeSamples() {
        assertEquals(3, OnboardingPromptOption.samples.size)
        assertEquals("ASK", OnboardingPromptOption.samples[0].label)
        assertEquals("WRITE", OnboardingPromptOption.samples[1].label)
        assertEquals("EXPLORE", OnboardingPromptOption.samples[2].label)
    }

    @Test
    fun onboardingQueueItem_hasFourSamplesWithCorrectStatuses() {
        assertEquals(4, OnboardingQueueItem.samples.size)
        assertEquals(OnboardingQueueItem.Status.RUNNING, OnboardingQueueItem.samples[0].status)
        assertEquals(OnboardingQueueItem.Status.NEXT, OnboardingQueueItem.samples[1].status)
        assertEquals(OnboardingQueueItem.Status.QUEUED, OnboardingQueueItem.samples[2].status)
        assertEquals(OnboardingQueueItem.Status.READY, OnboardingQueueItem.samples[3].status)
    }

    @Test
    fun onboardingPageType_hasAllCases() {
        assertEquals(5, OnboardingPageType.allCases.size)
    }

    @Test
    fun onboardingDemoDefaults_matchSwiftersPreviewValues() {
        assertEquals(0, OnboardingDemoDefaults.selectedPromptIndex)
        assertEquals(0.62, OnboardingDemoDefaults.reasoningLevel, 0.001)
        assertEquals(2, OnboardingDemoDefaults.queuedPromptCount)
        assertTrue(OnboardingDemoDefaults.pairingConfirmed)
    }
}
