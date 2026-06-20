package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.CredentialStore
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider

internal class InMemoryCredentialStore : CredentialStore {
    private val keys = mutableMapOf<SessionProvider, String>()

    override suspend fun loadApiKey(provider: SessionProvider): String? = keys[provider]
    override suspend fun saveApiKey(provider: SessionProvider, key: String) { keys[provider] = key }
    override suspend fun removeApiKey(provider: SessionProvider) { keys.remove(provider) }
}
