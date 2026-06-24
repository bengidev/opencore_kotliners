package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.home.contextwindow.core.ContextWindowTracker
import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.home.speedmode.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
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
    private val onSendMessage: ((String, String?) -> Unit)? = null,
    private val onNewConversation: (() -> Unit)? = null
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(HomeState())
    val state: Value<HomeState> = _state
    private var searchDebounceJob: Job? = null
    private var catalogReloadJob: Job? = null
    private var catalogLoadGeneration = 0
    private val contextWindowTracker = ContextWindowTracker()
    private var contextMessages: List<SidePanelMessage> = emptyList()
    private var contextDraft = ""

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        startCatalogReload(allowAutoSelect = false)
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
            onSendMessage?.invoke(message, current.activeProviderSortBy)
        }
    }
    fun onModelSelectorTapped() {
        dispatch(HomeIntent.ModelSelectorTapped)
        startCatalogReload(allowAutoSelect = false, onlyIfNeeded = true)
    }
    fun onModelPickerDismissed() = dispatch(HomeIntent.ModelPickerDismissed)
    fun onModelSelected(model: SidePanelModel) {
        scope.launch {
            preferenceStore.setModelId(model.id)
            dispatch(HomeIntent.ModelSelected(model))
            refreshStoredContextUsage()
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
    fun onSpeedModeSelected(mode: HomeComposerSpeedMode) = dispatch(HomeIntent.SpeedModeSelected(mode))

    fun refreshContextUsage(messages: List<SidePanelMessage>, draftMessage: String) {
        contextMessages = messages
        contextDraft = draftMessage
        refreshStoredContextUsage()
    }

    fun onProviderChanged() {
        ++catalogLoadGeneration
        startCatalogReload(allowAutoSelect = true)
    }

    fun onCredentialsChanged() {
        ++catalogLoadGeneration
        scope.launch {
            reloadApiKeyStatus()
            dispatch(HomeIntent.CatalogCleared)
            startCatalogReload(allowAutoSelect = _state.value.selectedModelId == null)
        }
    }

    private fun startCatalogReload(allowAutoSelect: Boolean, onlyIfNeeded: Boolean = false) {
        if (onlyIfNeeded && !shouldReloadCatalog()) return
        val generation = ++catalogLoadGeneration
        catalogReloadJob?.cancel()
        catalogReloadJob = scope.launch {
            dispatch(HomeIntent.ModelsLoadingStarted)
            reloadModelSelection(allowAutoSelect = allowAutoSelect, generation = generation)
        }
    }

    private fun shouldReloadCatalog(): Boolean {
        val current = _state.value
        return current.availableModels.isEmpty() ||
            (current.hasApiKey && !current.modelCatalogIsLive)
    }

    private suspend fun reloadModelSelection(allowAutoSelect: Boolean, generation: Int) {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val secret = credentialStore.secret(provider.id)
        val catalogResult = modelCatalogClient.listModels(provider, secret)
        if (generation != catalogLoadGeneration) return

        val models = catalogResult.models
        val selection = reconcileModelSelection(
            models = models,
            savedModelId = preference.modelId,
            allowAutoSelect = allowAutoSelect
        )
        if (selection.modelId != preference.modelId) {
            preferenceStore.setModelId(selection.modelId)
        }
        dispatch(
            HomeIntent.ModelSelectionLoaded(
                modelId = selection.modelId,
                modelTitle = selection.modelTitle,
                providerId = provider.id,
                models = models,
                catalogIsLive = catalogResult.isLive,
                catalogErrorHint = catalogResult.errorHint
            )
        )
        reloadApiKeyStatus()
        refreshStoredContextUsage()
    }

    private fun reconcileModelSelection(
        models: List<SidePanelModel>,
        savedModelId: String?,
        allowAutoSelect: Boolean
    ): ModelSelection = when {
        models.isEmpty() -> ModelSelection(modelId = null, modelTitle = null)
        savedModelId != null && models.any { it.id == savedModelId } -> {
            val model = models.first { it.id == savedModelId }
            ModelSelection(modelId = model.id, modelTitle = model.displayTitle)
        }
        allowAutoSelect || savedModelId != null -> {
            val model = models.first()
            ModelSelection(modelId = model.id, modelTitle = model.displayTitle)
        }
        else -> ModelSelection(modelId = null, modelTitle = null)
    }

    private fun refreshStoredContextUsage() {
        val contextLength = _state.value.availableModels
            .firstOrNull { it.id == _state.value.selectedModelId }
            ?.contextLength
        contextWindowTracker.refresh(
            messages = contextMessages,
            draft = contextDraft,
            contextLength = contextLength
        )
        dispatch(HomeIntent.ContextUsageUpdated(contextWindowTracker.usage))
    }

    private suspend fun reloadApiKeyStatus() {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val hasApiKey = !credentialStore.secret(provider.id).isNullOrBlank()
        dispatch(HomeIntent.CredentialsLoaded(hasApiKey))
    }

    private data class ModelSelection(
        val modelId: String?,
        val modelTitle: String?
    )
}
