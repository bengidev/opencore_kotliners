package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.sidepanel.domain.CredentialStore
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider
import io.github.bengidev.opencore.sidepanel.domain.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class SidePanelComponent(
    componentContext: ComponentContext,
    private val sessionRepository: SessionRepository,
    private val credentialStore: CredentialStore,
    private val onSettingsTappedCallback: () -> Unit = {}
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(SidePanelState())
    val state: Value<SidePanelState> = _state

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        dispatch(SidePanelIntent.OnAppear)
        scope.launch {
            val sessions = sessionRepository.loadSessions()
            dispatch(SidePanelIntent.SessionsLoaded(sessions))
            loadApiKeyForProvider(_state.value.selectedProvider)
        }
    }

    internal fun dispatch(intent: SidePanelIntent) {
        _state.update { current -> SidePanelReducer.reduce(current, intent) }
        when (intent) {
            is SidePanelIntent.SettingsTapped -> onSettingsTappedCallback()
            is SidePanelIntent.SaveApiKeyTapped -> scope.launch {
                val provider = _state.value.selectedProvider
                val key = _state.value.apiKeyInput
                if (key.isNotBlank()) {
                    credentialStore.saveApiKey(provider, key)
                    dispatch(SidePanelIntent.ApiKeySaved(provider))
                }
            }
            is SidePanelIntent.RemoveApiKeyTapped -> scope.launch {
                val provider = _state.value.selectedProvider
                credentialStore.removeApiKey(provider)
                dispatch(SidePanelIntent.ApiKeyRemoved(provider))
            }
            is SidePanelIntent.ProviderSelected -> scope.launch {
                loadApiKeyForProvider(intent.provider)
            }
            else -> Unit
        }
    }

    private suspend fun loadApiKeyForProvider(provider: SessionProvider) {
        val key = credentialStore.loadApiKey(provider)
        dispatch(SidePanelIntent.ApiKeyLoaded(provider, key))
    }

    internal fun onSessionTapped(id: String) = dispatch(SidePanelIntent.SessionSelected(id))
    internal fun onSessionRenamed(id: String, newTitle: String) {
        dispatch(SidePanelIntent.SessionRenamed(id, newTitle))
        scope.launch { sessionRepository.renameSession(id, newTitle) }
    }
    internal fun onSessionDeleted(id: String) {
        dispatch(SidePanelIntent.SessionDeleted(id))
        scope.launch { sessionRepository.deleteSession(id) }
    }
    internal fun onSettingsTapped() = dispatch(SidePanelIntent.SettingsTapped)
    internal fun onSettingsDismissed() = dispatch(SidePanelIntent.SettingsDismissed)
    internal fun onProviderSelected(provider: SessionProvider) = dispatch(SidePanelIntent.ProviderSelected(provider))
    internal fun onApiKeyChanged(value: String) = dispatch(SidePanelIntent.ApiKeyChanged(value))
    internal fun onSaveApiKeyTapped() = dispatch(SidePanelIntent.SaveApiKeyTapped)
    internal fun onRemoveApiKeyTapped() = dispatch(SidePanelIntent.RemoveApiKeyTapped)
}
