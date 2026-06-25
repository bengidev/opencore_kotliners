package io.github.bengidev.opencore.sidepanel.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
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
            val legacyReasoning = preferences[KEY_LEGACY_REASONING_MODEL]
            val wireValue = preferences[KEY_REASONING_EFFORT]
                ?: legacyReasoning?.let(::migrateLegacyReasoningWireValue)
            SidePanelProviderPreference(
                providerId = preferences[KEY_PROVIDER_ID],
                modelId = preferences[KEY_MODEL_ID],
                reasoningEffortWireValue = wireValue
            )
        }.first()

    override suspend fun setProviderId(id: String) {
        context.sidePanelPreferencesDataStore.edit { preferences ->
            preferences[KEY_PROVIDER_ID] = id
        }
    }

    override suspend fun setModelId(id: String?) {
        context.sidePanelPreferencesDataStore.edit { preferences ->
            if (id.isNullOrBlank()) {
                preferences.remove(KEY_MODEL_ID)
            } else {
                preferences[KEY_MODEL_ID] = id
            }
        }
    }

    override suspend fun setReasoningEffort(effort: ModelReasoningEffort) {
        context.sidePanelPreferencesDataStore.edit { preferences ->
            val wireValue = effort.wireValue
            if (wireValue.isNullOrBlank()) {
                preferences.remove(KEY_REASONING_EFFORT)
            } else {
                preferences[KEY_REASONING_EFFORT] = wireValue
            }
            preferences.remove(KEY_LEGACY_REASONING_MODEL)
        }
    }

    companion object {
        private val KEY_PROVIDER_ID = stringPreferencesKey("opencore.provider.selectedProviderID")
        private val KEY_MODEL_ID = stringPreferencesKey("opencore.provider.selectedModelID")
        private val KEY_REASONING_EFFORT = stringPreferencesKey("opencore.provider.reasoningEffortWireValue")
        private val KEY_LEGACY_REASONING_MODEL = stringPreferencesKey("opencore.provider.reasoningModel")

        private fun migrateLegacyReasoningWireValue(legacy: String): String? = when (legacy.lowercase()) {
            "off" -> null
            "low", "medium", "high" -> legacy.lowercase()
            else -> "high"
        }
    }
}
