package io.github.bengidev.opencore.shared.credential

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Production [CredentialStoring] backed by EncryptedSharedPreferences. */
internal class CredentialEncryptedStore(
    context: Context
) : CredentialStoring {

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun secret(forProviderId: String): String? =
        preferences.getString(accountKey(forProviderId), null)

    override fun save(secret: String, forProviderId: String) {
        preferences.edit().putString(accountKey(forProviderId), secret).apply()
    }

    override fun clear(forProviderId: String) {
        preferences.edit().remove(accountKey(forProviderId)).apply()
    }

    private fun accountKey(providerId: String): String = "$providerId-api-key"

    companion object {
        private const val PREFS_NAME = "opencore_sidepanel_credentials"
    }
}
