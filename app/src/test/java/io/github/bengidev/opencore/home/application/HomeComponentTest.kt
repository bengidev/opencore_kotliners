package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun homeComponent(
        lifecycle: LifecycleRegistry,
        preferenceStore: InMemorySidePanelPreferenceStore = InMemorySidePanelPreferenceStore(
            SidePanelProviderPreference(modelId = "meta-llama/llama-3.3-70b-instruct:free")
        ),
        credentialStore: InMemorySidePanelCredentialStore = HomeTestCatalog.credentialStoreWithKey(),
        modelCatalogClient: HomeModelCatalogClient = HomeTestCatalog.catalogClient(
            ioDispatcher = testDispatcher
        ),
        onSendMessage: ((String, String?) -> Unit)? = null,
        onNewConversation: (() -> Unit)? = null
    ) = HomeComponent(
        componentContext = DefaultComponentContext(lifecycle),
        preferenceStore = preferenceStore,
        credentialStore = credentialStore,
        modelCatalogClient = modelCatalogClient,
        onSendMessage = onSendMessage,
        onNewConversation = onNewConversation
    )

    @Test
    fun sendTapped_invokesCallbackWithTrimmedDraft() = runTest(testDispatcher) {
        var sent: String? = null
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            onSendMessage = { message, _ -> sent = message }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("  Hello  ")
        component.onSendTapped()

        assertEquals("Hello", sent)
        assertTrue(component.state.value.draftMessage.isEmpty())
    }

    @Test
    fun sendTapped_blankDraft_doesNotInvokeCallback() = runTest(testDispatcher) {
        var sent = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            onSendMessage = { _: String, _: String? -> sent = true }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("   ")
        component.onSendTapped()

        assertFalse(sent)
    }

    @Test
    fun sendTapped_withoutApiKey_doesNotInvokeCallback() = runTest(testDispatcher) {
        var sent = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            credentialStore = InMemorySidePanelCredentialStore(),
            onSendMessage = { _: String, _: String? -> sent = true }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("Hello")
        component.onSendTapped()

        assertFalse(sent)
        assertEquals("Hello", component.state.value.draftMessage)
    }

    @Test
    fun onCredentialsChanged_refreshesApiKeyState() = runTest(testDispatcher) {
        val store = InMemorySidePanelCredentialStore()
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            credentialStore = store,
            modelCatalogClient = HomeTestCatalog.catalogClient(ioDispatcher = testDispatcher)
        )
        advanceUntilIdle()
        assertFalse(component.state.value.hasApiKey)

        store.save("sk-test", SidePanelProviderApi.openRouter.id)
        component.onCredentialsChanged()
        advanceUntilIdle()

        assertTrue(component.state.value.hasApiKey)
    }

    @Test
    fun modelSelected_persistsSelection() = runTest(testDispatcher) {
        val store = InMemorySidePanelPreferenceStore()
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            preferenceStore = store
        )
        advanceUntilIdle()

        val model = component.state.value.availableModels.first()
        component.onModelSelected(model)
        advanceUntilIdle()

        assertEquals(model.id, store.preference().modelId)
        assertEquals(model.displayTitle, component.state.value.selectedModelTitle)
    }

    @Test
    fun catalogReload_staleLoadDoesNotOverwriteNewerProvider() = runTest(testDispatcher) {
        val openRouterBody = """
            {
              "data": [
                {
                  "id": "openrouter/free",
                  "name": "Free Models Router",
                  "architecture": { "modality": "text", "tokenizer": "Router" },
                  "pricing": { "prompt": "0", "completion": "0" }
                }
              ]
            }
        """.trimIndent()
        val openCodeBody = """
            {
              "data": [
                {
                  "id": "gpt-4o-mini",
                  "name": "GPT-4o Mini",
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "1", "completion": "1" }
                }
              ]
            }
        """.trimIndent()
        var requestCount = 0
        val catalogClient = HomeModelCatalogClient(
            httpGet = { _, _ ->
                requestCount++
                if (requestCount == 1) {
                    delay(1_000)
                    HomeModelCatalogClient.HttpGetResult(statusCode = 200, body = openRouterBody)
                } else {
                    HomeModelCatalogClient.HttpGetResult(statusCode = 200, body = openCodeBody)
                }
            },
            ioDispatcher = testDispatcher
        )
        val preferenceStore = InMemorySidePanelPreferenceStore(
            SidePanelProviderPreference(
                providerId = SidePanelProviderApi.openRouter.id,
                modelId = "openrouter/free"
            )
        )
        val credentialStore = InMemorySidePanelCredentialStore().apply {
            save("sk-test", SidePanelProviderApi.openRouter.id)
            save("sk-test", SidePanelProviderApi.openCode.id)
        }
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            preferenceStore = preferenceStore,
            credentialStore = credentialStore,
            modelCatalogClient = catalogClient
        )
        advanceUntilIdle()

        preferenceStore.setProviderId(SidePanelProviderApi.openCode.id)
        component.onProviderChanged()
        advanceUntilIdle()

        assertEquals(SidePanelProviderApi.openCode.id, component.state.value.selectedProviderId)
        assertEquals("gpt-4o-mini", component.state.value.selectedModelId)
    }

    @Test
    fun withoutApiKey_catalogIsEmptyAndModelUnavailable() = runTest(testDispatcher) {
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            credentialStore = InMemorySidePanelCredentialStore(),
            modelCatalogClient = HomeModelCatalogClient(ioDispatcher = testDispatcher)
        )
        advanceUntilIdle()

        assertTrue(component.state.value.availableModels.isEmpty())
        assertNull(component.state.value.selectedModelId)
        assertEquals("Not Available", component.state.value.modelPickerTitle)
    }

    @Test
    fun stalePersistedModelId_isReplacedFromLiveCatalog() = runTest(testDispatcher) {
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = homeComponent(
            lifecycle = lifecycle,
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = "stale/model-id")
            )
        )
        advanceUntilIdle()

        assertEquals(component.state.value.availableModels.first().id, component.state.value.selectedModelId)
    }
}
