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

    override suspend fun preference(): SettingsContextCompactionPreference =
        context.compactionPreferencesDataStore.data.map { preferences ->
            SettingsContextCompactionPreference(
                isEnabled = preferences[KEY_ENABLED] ?: true,
                triggerThresholdPercent = preferences[KEY_TRIGGER_THRESHOLD] ?: 90,
                minRecentMessages = preferences[KEY_MIN_RECENT_MESSAGES] ?: 4,
                reserveTokens = preferences[KEY_RESERVE_TOKENS] ?: 16_384,
                keepRecentTokens = preferences[KEY_KEEP_RECENT_TOKENS] ?: 20_000,
            )
        }.first()

    override suspend fun setEnabled(enabled: Boolean) {
        context.compactionPreferencesDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun setReserveTokens(tokens: Int) {
        context.compactionPreferencesDataStore.edit { it[KEY_RESERVE_TOKENS] = tokens }
    }

    override suspend fun setKeepRecentTokens(tokens: Int) {
        context.compactionPreferencesDataStore.edit { it[KEY_KEEP_RECENT_TOKENS] = tokens }
    }

    override suspend fun setMinRecentMessages(count: Int) {
        context.compactionPreferencesDataStore.edit { it[KEY_MIN_RECENT_MESSAGES] = count }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("compaction.enabled")
        private val KEY_TRIGGER_THRESHOLD = intPreferencesKey("compaction.triggerThresholdPercent")
        private val KEY_MIN_RECENT_MESSAGES = intPreferencesKey("compaction.minRecentMessages")
        private val KEY_RESERVE_TOKENS = intPreferencesKey("compaction.reserveTokens")
        private val KEY_KEEP_RECENT_TOKENS = intPreferencesKey("compaction.keepRecentTokens")
    }
}
