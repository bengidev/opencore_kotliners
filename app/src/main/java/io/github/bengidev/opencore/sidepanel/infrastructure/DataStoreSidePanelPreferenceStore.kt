package io.github.bengidev.opencore.sidepanel.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sidePanelPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sidepanel_prefs"
)

internal class DataStoreSidePanelPreferenceStore(
    private val context: Context
) : SidePanelPreferenceStore {

    override suspend fun preference(): SidePanelProviderPreference =
        context.sidePanelPreferencesDataStore.data.map { preferences ->
            SidePanelProviderPreference(
                providerId = preferences[KEY_PROVIDER_ID],
                reasoningModel = SidePanelReasoningModel.fromWire(preferences[KEY_REASONING_MODEL])
            )
        }.first()

    override suspend fun setProviderId(id: String) {
        context.sidePanelPreferencesDataStore.edit { preferences ->
            preferences[KEY_PROVIDER_ID] = id
        }
    }

    override suspend fun setReasoningModel(model: SidePanelReasoningModel) {
        context.sidePanelPreferencesDataStore.edit { preferences ->
            preferences[KEY_REASONING_MODEL] = model.name.lowercase()
        }
    }

    companion object {
        private val KEY_PROVIDER_ID = stringPreferencesKey("opencore.provider.selectedProviderID")
        private val KEY_REASONING_MODEL = stringPreferencesKey("opencore.provider.reasoningModel")
    }
}
