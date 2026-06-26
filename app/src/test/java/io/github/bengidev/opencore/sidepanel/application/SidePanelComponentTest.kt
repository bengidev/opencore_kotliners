package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.shared.credential.CredentialInMemoryStore
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
import org.junit.Assert.assertEquals
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
        val credentialStore = CredentialInMemoryStore().apply {
            save("sk-live", ProviderDescriptor.openRouter.id)
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
    fun dismissSettings_doesNotNotifyCredentialsChanged() = runTest(testDispatcher) {
        var credentialsChanged = 0
        val component = createComponent()
        component.onCredentialsChanged = { credentialsChanged++ }

        component.settingsButtonTapped()
        advanceUntilIdle()
        component.dismissSettings()
        advanceUntilIdle()

        assertFalse(component.showSettings.value)
        assertEquals(0, credentialsChanged)
    }

    @Test
    fun settingSaveAndClear_updatesStoredKeyIndicator() = runTest(testDispatcher) {
        val credentialStore = CredentialInMemoryStore()
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
        credentialStore: CredentialInMemoryStore = CredentialInMemoryStore()
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
