package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeComponentTest {

    @Test
    fun sendTapped_invokesCallbackWithTrimmedDraft() {
        var sent: String? = null
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            onSendMessage = { sent = it }
        )

        component.onDraftMessageChanged("  Hello  ")
        component.onSendTapped()

        assertEquals("Hello", sent)
        assertTrue(component.state.value.draftMessage.isEmpty())
    }

    @Test
    fun sendTapped_blankDraft_doesNotInvokeCallback() {
        var sent = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            onSendMessage = { sent = true }
        )

        component.onDraftMessageChanged("   ")
        component.onSendTapped()

        assertEquals(false, sent)
    }
}
