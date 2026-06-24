package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModelCatalog
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class HomeComponent(
    componentContext: ComponentContext,
    private val preferenceStore: SidePanelPreferenceStore,
    private val credentialStore: SidePanelCredentialStore,
    private val modelCatalogClient: HomeModelCatalogClient = HomeModelCatalogClient(),
    private val onSendMessage: ((String) -> Unit)? = null,
    private val onNewConversation: (() -> Unit)? = null
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(HomeState())
    val state: Value<HomeState> = _state
    private var searchDebounceJob: Job? = null
    private var catalogReloadJob: Job? = null
    private var catalogLoadGeneration = 0

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        startCatalogReload(resetToDefault = false)
    }

    fun dispatch(intent: HomeIntent) {
        _state.update { current -> HomeReducer.reduce(current, intent) }
    }

    fun onDraftMessageChanged(value: String) = dispatch(HomeIntent.DraftMessageChanged(value))
    fun onSidebarTapped() = dispatch(HomeIntent.SidebarTapped)
    fun onNewConversationTapped() {
        onNewConversation?.invoke()
        dispatch(HomeIntent.NewConversationTapped)
    }
    fun onAttachmentTapped() = dispatch(HomeIntent.AttachmentTapped)
    fun onMicrophoneTapped() = dispatch(HomeIntent.MicrophoneTapped)
    fun onSendTapped() {
        val current = _state.value
        val message = current.draftMessage.trim()
        dispatch(HomeIntent.SendTapped)
        if (message.isNotEmpty() && current.selectedModelId != null && current.hasApiKey) {
            onSendMessage?.invoke(message)
        }
    }
    fun onModelSelectorTapped() {
        dispatch(HomeIntent.ModelSelectorTapped)
        startCatalogReload(resetToDefault = false, onlyIfNeeded = true)
    }
    fun onModelPickerDismissed() = dispatch(HomeIntent.ModelPickerDismissed)
    fun onModelSelected(model: SidePanelModel) {
        scope.launch {
            preferenceStore.setModelId(model.id)
            dispatch(HomeIntent.ModelSelected(model))
        }
    }
    fun onModelSearchQueryChanged(query: String) {
        dispatch(HomeIntent.ModelSearchQueryChanged(query))
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            delay(300)
            dispatch(HomeIntent.ModelSearchQueryApplied(query))
        }
    }
    fun onModelFilterFreeOnlyChanged(enabled: Boolean) =
        dispatch(HomeIntent.ModelFilterFreeOnlyChanged(enabled))
    fun onSpeedModeTapped() = dispatch(HomeIntent.SpeedModeTapped)
    fun onContextUsageTapped() = dispatch(HomeIntent.ContextUsageTapped)

    fun onProviderChanged() {
        ++catalogLoadGeneration
        startCatalogReload(resetToDefault = true)
    }

    fun onCredentialsChanged() {
        ++catalogLoadGeneration
        scope.launch {
            reloadApiKeyStatus()
            startCatalogReload(resetToDefault = false)
        }
    }

    private fun startCatalogReload(resetToDefault: Boolean, onlyIfNeeded: Boolean = false) {
        if (onlyIfNeeded && !shouldReloadCatalog()) return
        val generation = ++catalogLoadGeneration
        catalogReloadJob?.cancel()
        catalogReloadJob = scope.launch {
            dispatch(HomeIntent.ModelsLoadingStarted)
            reloadModelSelection(resetToDefault = resetToDefault, generation = generation)
        }
    }

    private fun shouldReloadCatalog(): Boolean {
        val current = _state.value
        return current.availableModels.isEmpty() ||
            (current.hasApiKey && !current.modelCatalogIsLive)
    }

    private suspend fun reloadModelSelection(resetToDefault: Boolean, generation: Int) {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val secret = credentialStore.secret(provider.id)
        val catalogResult = modelCatalogClient.listModels(provider, secret)
        if (generation != catalogLoadGeneration) return

        val models = catalogResult.models
        val modelId = when {
            resetToDefault -> SidePanelModelCatalog.defaultModel(provider).id
            preference.modelId != null && models.any { it.id == preference.modelId } ->
                preference.modelId
            else -> SidePanelModelCatalog.defaultModel(provider).id
        }
        if (modelId != preference.modelId) {
            preferenceStore.setModelId(modelId)
        }
        val selectedModel = models.firstOrNull { it.id == modelId }
        val title = selectedModel?.displayTitle
            ?: SidePanelModelCatalog.displayTitle(provider.id, modelId)
        dispatch(
            HomeIntent.ModelSelectionLoaded(
                modelId = modelId,
                modelTitle = title,
                providerId = provider.id,
                models = models,
                catalogIsLive = catalogResult.isLive,
                catalogErrorHint = catalogResult.errorHint
            )
        )
        reloadApiKeyStatus()
    }

    private suspend fun reloadApiKeyStatus() {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val hasApiKey = !credentialStore.secret(provider.id).isNullOrBlank()
        dispatch(HomeIntent.CredentialsLoaded(hasApiKey))
    }
}
