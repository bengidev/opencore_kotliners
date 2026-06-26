package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.home.utilities.ContextWindowTracker
import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import io.github.bengidev.opencore.shared.credential.CredentialStoring
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
    private val credentialStore: CredentialStoring,
    private val modelCatalogClient: HomeModelCatalogClient = HomeModelCatalogClient(),
    private val onSendMessage: ((String, String?, String?) -> Unit)? = null,
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
        scope.launch {
            val preference = preferenceStore.preference()
            dispatch(HomeIntent.ReasoningEffortWireValueUpdated(preference.reasoningEffortWireValue))
        }
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
            onSendMessage?.invoke(message, current.activeProviderSortBy, current.activeReasoningEffort)
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
    fun onReasoningEffortSelected(effort: ModelReasoningEffort) {
        scope.launch {
            preferenceStore.setReasoningEffort(effort)
            dispatch(HomeIntent.ReasoningEffortSelected(effort))
        }
    }

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
        val providerId = preference.providerId ?: ProviderRegistry.defaultAdapter.descriptor.id
        val adapter = ProviderRegistry.resolve(providerId)
        val secret = credentialStore.secret(providerId)
        val catalogResult = modelCatalogClient.listModels(adapter, secret)
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
                providerId = providerId,
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
        val providerId = preference.providerId ?: ProviderRegistry.defaultAdapter.descriptor.id
        val hasApiKey = !credentialStore.secret(providerId).isNullOrBlank()
        dispatch(HomeIntent.CredentialsLoaded(hasApiKey))
    }
}
