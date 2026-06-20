package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemoryCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySessionRepository
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
class SidePanelComponentTest {

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
    fun settingsButtonTapped_showsSettingsAndNotifiesHost() = runTest(testDispatcher) {
        var hostNotified = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = SidePanelComponent(
            componentContext = DefaultComponentContext(lifecycle),
            sessionRepository = InMemorySessionRepository(),
            credentialStore = InMemoryCredentialStore(),
            onSettingsTappedCallback = { hostNotified = true }
        )

        advanceUntilIdle()
        assertFalse(component.state.value.isSettingsVisible)

        component.onSettingsTapped()

        assertTrue(component.state.value.isSettingsVisible)
        assertTrue(hostNotified)
    }
}
