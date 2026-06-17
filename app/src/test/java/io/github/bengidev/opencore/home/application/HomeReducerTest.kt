package io.github.bengidev.opencore.home.application

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {

    @Test
    fun draftMessageChanged_updatesDraft() {
        val result = HomeReducer.reduce(
            HomeState(),
            HomeIntent.DraftMessageChanged("Hello")
        )
        assertEquals("Hello", result.draftMessage)
        assertTrue(result.canSend)
    }

    @Test
    fun sendTapped_clearsDraftWhenNotBlank() {
        val result = HomeReducer.reduce(
            HomeState(draftMessage = "Hello"),
            HomeIntent.SendTapped
        )
        assertEquals("", result.draftMessage)
        assertFalse(result.canSend)
    }

    @Test
    fun sendTapped_isNoOpWhenDraftBlank() {
        val state = HomeState(draftMessage = "   ")
        val result = HomeReducer.reduce(state, HomeIntent.SendTapped)
        assertEquals(state, result)
    }

    @Test
    fun placeholderIntents_areNoOps() {
        val state = HomeState(draftMessage = "Draft")
        val intents = listOf(
            HomeIntent.AttachmentTapped,
            HomeIntent.ContextUsageTapped,
            HomeIntent.MicrophoneTapped,
            HomeIntent.ModelSelectorTapped,
            HomeIntent.NewConversationTapped,
            HomeIntent.SidebarTapped,
            HomeIntent.SpeedModeTapped
        )

        intents.forEach { intent ->
            assertEquals(state, HomeReducer.reduce(state, intent))
        }
    }
}
