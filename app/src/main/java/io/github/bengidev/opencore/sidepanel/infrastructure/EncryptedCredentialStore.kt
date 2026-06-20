package io.github.bengidev.opencore.sidepanel.infrastructure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.bengidev.opencore.sidepanel.domain.CredentialStore
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider

internal class EncryptedCredentialStore(
    private val context: Context
) : CredentialStore {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun loadApiKey(provider: SessionProvider): String? =
        prefs.getString(storageKey(provider), null)

    override suspend fun saveApiKey(provider: SessionProvider, key: String) {
        prefs.edit().putString(storageKey(provider), key).apply()
    }

    override suspend fun removeApiKey(provider: SessionProvider) {
        prefs.edit().remove(storageKey(provider)).apply()
    }

    companion object {
        private const val FILE_NAME = "opencore_credentials"
    }
}

private fun storageKey(provider: SessionProvider): String =
    "${provider.name.lowercase()}-api-key"
