package io.github.bengidev.opencore.sidepanel.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.compactionPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "context_compaction_prefs",
)

internal class DataStoreSettingsContextCompactionPreferenceStore(
    private val context: Context,
) : SettingsContextCompactionPreferenceStore {

    override suspend fun preference(): SettingsContextCompactionPreference {
        val loaded = context.compactionPreferencesDataStore.data.map { preferences ->
            SettingsContextCompactionPreference(
                isEnabled = preferences[KEY_ENABLED] ?: true,
                triggerThresholdPercent = preferences[KEY_TRIGGER_THRESHOLD] ?: 90,
                minRecentMessages = preferences[KEY_MIN_RECENT_MESSAGES] ?: 4,
                reserveTokens = preferences.decodeReserveTokens(),
                keepRecentTokens = preferences.decodeKeepRecentTokens(),
            )
        }.first()

        val normalized = loaded.normalizeAfterDecoding()
        if (normalized != loaded) {
            persist(normalized)
        }
        return normalized
    }

    override suspend fun setEnabled(enabled: Boolean) {
        context.compactionPreferencesDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun setThresholdPercent(percent: Int) {
        val current = preference()
        persist(current.withThresholdPercent(percent))
    }

    override suspend fun setMinRecentMessages(count: Int) {
        context.compactionPreferencesDataStore.edit { it[KEY_MIN_RECENT_MESSAGES] = count }
    }

    private suspend fun persist(preference: SettingsContextCompactionPreference) {
        context.compactionPreferencesDataStore.edit { preferences ->
            preferences[KEY_ENABLED] = preference.isEnabled
            preferences[KEY_TRIGGER_THRESHOLD] = preference.triggerThresholdPercent
            preferences[KEY_MIN_RECENT_MESSAGES] = preference.minRecentMessages
            preferences[KEY_RESERVE_TOKENS] = preference.reserveTokens
            preferences[KEY_KEEP_RECENT_TOKENS] = preference.keepRecentTokens
        }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("compaction.enabled")
        private val KEY_TRIGGER_THRESHOLD = intPreferencesKey("compaction.triggerThresholdPercent")
        private val KEY_MIN_RECENT_MESSAGES = intPreferencesKey("compaction.minRecentMessages")
        private val KEY_RESERVE_TOKENS = intPreferencesKey("compaction.reserveTokens")
        private val KEY_KEEP_RECENT_TOKENS = intPreferencesKey("compaction.keepRecentTokens")

        private fun Preferences.decodeReserveTokens(): Int =
            if (contains(KEY_RESERVE_TOKENS)) {
                this[KEY_RESERVE_TOKENS] ?: 0
            } else {
                SettingsContextCompactionPreference.LEGACY_DEFAULT_RESERVE_TOKENS
            }

        private fun Preferences.decodeKeepRecentTokens(): Int =
            if (contains(KEY_KEEP_RECENT_TOKENS)) {
                this[KEY_KEEP_RECENT_TOKENS] ?: 0
            } else {
                SettingsContextCompactionPreference.LEGACY_DEFAULT_KEEP_RECENT_TOKENS
            }
    }
}
