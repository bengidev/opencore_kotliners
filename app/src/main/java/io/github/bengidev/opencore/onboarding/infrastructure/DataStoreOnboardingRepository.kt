package io.github.bengidev.opencore.onboarding.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DataStoreOnboardingRepository(
    private val context: Context
) : OnboardingRepository {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "onboarding_prefs"
    )

    override suspend fun isOnboardingCompleted(): Boolean =
        context.dataStore.data.map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }.first()

    override suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
            preferences[KEY_COMPLETED_AT] = System.currentTimeMillis()
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_COMPLETED_AT = longPreferencesKey("onboarding_completed_at")
    }
}
