package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun settingsButtonTapped_presentsSettingWithStoredKeyStatus() = runTest(testDispatcher) {
        val credentialStore = InMemorySidePanelCredentialStore().apply {
            save("sk-live", SidePanelProviderApi.default.id)
        }
        val component = createComponent(credentialStore = credentialStore)

        component.settingsButtonTapped()
        advanceUntilIdle()

        assertTrue(component.showSettings.value)
        val setting = component.setting
        assertNotNull(setting)
        setting!!.onAppear()
        advanceUntilIdle()

        assertTrue(setting.state.value.hasStoredKey)
    }

    @Test
    fun settingsButtonTapped_canReopenAfterDismiss() = runTest(testDispatcher) {
        val component = createComponent()

        component.settingsButtonTapped()
        advanceUntilIdle()
        assertTrue(component.showSettings.value)

        component.dismissSettings()
        advanceUntilIdle()
        assertFalse(component.showSettings.value)

        component.settingsButtonTapped()
        advanceUntilIdle()
        assertTrue(component.showSettings.value)
    }

    @Test
    fun settingSaveAndClear_updatesStoredKeyIndicator() = runTest(testDispatcher) {
        val credentialStore = InMemorySidePanelCredentialStore()
        val component = createComponent(credentialStore = credentialStore)
        component.settingsButtonTapped()
        advanceUntilIdle()

        val setting = requireNotNull(component.setting)
        setting.onAppear()
        advanceUntilIdle()
        assertFalse(setting.state.value.hasStoredKey)

        setting.onDraftChanged("sk-test")
        setting.save()
        advanceUntilIdle()
        assertTrue(setting.state.value.hasStoredKey)

        setting.clear()
        advanceUntilIdle()
        assertFalse(setting.state.value.hasStoredKey)
    }

    private fun createComponent(
        credentialStore: InMemorySidePanelCredentialStore = InMemorySidePanelCredentialStore()
    ): SidePanelComponent {
        val lifecycle = LifecycleRegistry().apply { resume() }
        return SidePanelComponent(
            componentContext = DefaultComponentContext(lifecycle),
            history = InMemorySidePanelHistoryRepository(seed = emptyList()),
            credentialStore = credentialStore,
            preferenceStore = InMemorySidePanelPreferenceStore()
        )
    }
}
