package io.github.bengidev.opencore.sidepanel.infrastructure

internal class InMemorySidePanelCredentialStore : SidePanelCredentialStore {
    private val secrets = mutableMapOf<String, String>()

    override fun secret(forProviderId: String): String? = secrets[forProviderId]

    override fun save(secret: String, forProviderId: String) {
        secrets[forProviderId] = secret
    }

    override fun clear(forProviderId: String) {
        secrets.remove(forProviderId)
    }
}
