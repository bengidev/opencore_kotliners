package io.github.bengidev.opencore.sidepanel.application.setting

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class SidePanelSettingComponent(
    componentContext: ComponentContext,
    private val credentialStore: CredentialStoring,
    private val preferenceStore: SidePanelPreferenceStore,
    modelSupportsReasoning: Boolean = false,
    availableReasoningEfforts: List<ModelReasoningEffort> = emptyList(),
    initialState: SidePanelSettingState = SidePanelSettingState(
        modelSupportsReasoning = modelSupportsReasoning,
        availableReasoningEfforts = availableReasoningEfforts
    )
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(initialState)
    val state: Value<SidePanelSettingState> = _state

    var onCredentialsChanged: (() -> Unit)? = null
    var onReasoningEffortChanged: (() -> Unit)? = null
    var onProviderChanged: ((String) -> Unit)? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    fun dispatch(intent: SidePanelSettingIntent) {
        _state.update { current -> SidePanelSettingReducer.reduce(current, intent) }
    }

    fun onAppear() {
        scope.launch {
            val preference = preferenceStore.preference()
            val providerId = preference.providerId ?: ProviderRegistry.defaultAdapter.descriptor.id
            val available = _state.value.availableReasoningEfforts
            val resolved = ModelReasoningEffort.resolvedSelection(
                storedWireValue = preference.reasoningEffortWireValue,
                available = available
            )
            dispatch(
                SidePanelSettingIntent.Appeared(
                    selectedProviderId = providerId,
                    hasStoredKey = credentialStore.secret(providerId) != null,
                    reasoningEffort = resolved,
                    availableReasoningEfforts = available
                )
            )
        }
    }

    fun onDraftChanged(value: String) = dispatch(SidePanelSettingIntent.DraftChanged(value))

    fun save() {
        val key = _state.value.draftApiKey.trim()
        if (key.isEmpty()) return
        val providerId = _state.value.selectedProviderId
        runCatching {
            credentialStore.save(key, providerId)
        }.onSuccess {
            dispatch(SidePanelSettingIntent.SaveSucceeded)
            onCredentialsChanged?.invoke()
        }.onFailure {
            dispatch(SidePanelSettingIntent.SaveFailed("Could not save the key to secure storage."))
        }
    }

    fun clear() {
        val providerId = _state.value.selectedProviderId
        runCatching {
            credentialStore.clear(providerId)
        }.onSuccess {
            dispatch(SidePanelSettingIntent.ClearSucceeded)
            onCredentialsChanged?.invoke()
        }.onFailure {
            dispatch(SidePanelSettingIntent.ClearFailed("Could not remove the key from secure storage."))
        }
    }

    fun selectReasoningEffort(effort: ModelReasoningEffort) {
        scope.launch {
            preferenceStore.setReasoningEffort(effort)
            dispatch(SidePanelSettingIntent.ReasoningEffortSelected(effort))
            onReasoningEffortChanged?.invoke()
        }
    }

    fun selectProvider(id: String) {
        scope.launch {
            preferenceStore.setProviderId(id)
            dispatch(
                SidePanelSettingIntent.ProviderSelected(
                    id = id,
                    hasStoredKey = credentialStore.secret(id) != null
                )
            )
            onProviderChanged?.invoke(id)
        }
    }

    fun updateReasoningOptions(
        availableReasoningEfforts: List<ModelReasoningEffort>,
        modelSupportsReasoning: Boolean
    ) {
        dispatch(
            SidePanelSettingIntent.ReasoningOptionsUpdated(
                availableReasoningEfforts = availableReasoningEfforts,
                modelSupportsReasoning = modelSupportsReasoning
            )
        )
    }
}
