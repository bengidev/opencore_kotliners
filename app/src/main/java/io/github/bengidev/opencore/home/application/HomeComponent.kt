package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModelCatalog
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class HomeComponent(
    componentContext: ComponentContext,
    private val preferenceStore: SidePanelPreferenceStore,
    private val onSendMessage: ((String) -> Unit)? = null,
    private val onNewConversation: (() -> Unit)? = null
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(HomeState())
    val state: Value<HomeState> = _state

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        scope.launch { reloadModelSelection(resetToDefault = false) }
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
        val message = _state.value.draftMessage.trim()
        dispatch(HomeIntent.SendTapped)
        if (message.isNotEmpty() && _state.value.selectedModelId != null) {
            onSendMessage?.invoke(message)
        }
    }
    fun onModelSelectorTapped() {
        dispatch(HomeIntent.ModelSelectorTapped)
        scope.launch { ensureModelsLoaded() }
    }
    fun onModelPickerDismissed() = dispatch(HomeIntent.ModelPickerDismissed)
    fun onModelSelected(model: SidePanelModel) {
        scope.launch {
            preferenceStore.setModelId(model.id)
            dispatch(HomeIntent.ModelSelected(model))
        }
    }
    fun onSpeedModeTapped() = dispatch(HomeIntent.SpeedModeTapped)
    fun onContextUsageTapped() = dispatch(HomeIntent.ContextUsageTapped)

    fun onProviderChanged() {
        scope.launch { reloadModelSelection(resetToDefault = true) }
    }

    private suspend fun reloadModelSelection(resetToDefault: Boolean) {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val models = SidePanelModelCatalog.modelsFor(provider)
        val modelId = when {
            resetToDefault -> SidePanelModelCatalog.defaultModel(provider).id
            preference.modelId != null && models.any { it.id == preference.modelId } ->
                preference.modelId
            else -> SidePanelModelCatalog.defaultModel(provider).id
        }
        if (modelId != preference.modelId) {
            preferenceStore.setModelId(modelId)
        }
        val title = SidePanelModelCatalog.displayTitle(provider.id, modelId)
        dispatch(
            HomeIntent.ModelSelectionLoaded(
                modelId = modelId,
                modelTitle = title,
                models = models
            )
        )
    }

    private suspend fun ensureModelsLoaded() {
        if (_state.value.availableModels.isNotEmpty()) return
        reloadModelSelection(resetToDefault = false)
    }
}
