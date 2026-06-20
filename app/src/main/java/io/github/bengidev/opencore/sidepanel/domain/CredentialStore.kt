package io.github.bengidev.opencore.sidepanel.domain

internal interface CredentialStore {
    suspend fun loadApiKey(provider: SessionProvider): String?
    suspend fun saveApiKey(provider: SessionProvider, key: String)
    suspend fun removeApiKey(provider: SessionProvider)
}
