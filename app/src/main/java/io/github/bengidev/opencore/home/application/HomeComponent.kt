package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.home.utilities.ContextWindowTracker
import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
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

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        startCatalogReload(autoSelectWhenNoneSaved = false)
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
        if (message.isNotEmpty() && current.canSend) {
            onSendMessage?.invoke(message, current.activeProviderSortBy)
        }
    }
    fun onModelSelectorTapped() {
        dispatch(HomeIntent.ModelSelectorTapped)
        startCatalogReload(autoSelectWhenNoneSaved = false, onlyIfNeeded = true)
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

    fun refreshContextUsage(messages: List<SidePanelMessage>) {
        contextMessages = messages
        refreshStoredContextUsage()
    }

    fun onProviderChanged() {
        ++catalogLoadGeneration
        startCatalogReload(autoSelectWhenNoneSaved = true)
    }

    fun onCredentialsChanged() {
        ++catalogLoadGeneration
        scope.launch {
            reloadApiKeyStatus()
            startCatalogReload(autoSelectWhenNoneSaved = _state.value.selectedModelId == null)
        }
    }

    private fun startCatalogReload(autoSelectWhenNoneSaved: Boolean, onlyIfNeeded: Boolean = false) {
        if (onlyIfNeeded && !shouldReloadCatalog()) return
        val generation = ++catalogLoadGeneration
        catalogReloadJob?.cancel()
        catalogReloadJob = scope.launch {
            dispatch(HomeIntent.ModelsLoadingStarted)
            reloadModelSelection(autoSelectWhenNoneSaved = autoSelectWhenNoneSaved, generation = generation)
        }
    }

    private fun shouldReloadCatalog(): Boolean {
        val current = _state.value
        return current.availableModels.isEmpty() ||
            (current.hasApiKey && !current.modelCatalogIsLive)
    }

    private suspend fun reloadModelSelection(autoSelectWhenNoneSaved: Boolean, generation: Int) {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val secret = credentialStore.secret(provider.id)
        val catalogResult = modelCatalogClient.listModels(provider, secret)
        if (generation != catalogLoadGeneration) return

        val models = catalogResult.models
        val selection = ModelSelectionPolicy.reconcile(
            models = models,
            savedModelId = preference.modelId,
            autoSelectWhenNoneSaved = autoSelectWhenNoneSaved
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

    private fun refreshStoredContextUsage() {
        val current = _state.value
        val contextLength = current.availableModels
            .firstOrNull { it.id == current.selectedModelId }
            ?.contextLength
        contextWindowTracker.refresh(
            messages = contextMessages,
            draft = current.draftMessage,
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
}
