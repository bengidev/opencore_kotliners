package io.github.bengidev.opencore.shared.credential

/** In-memory [CredentialStoring] test double. */
internal class CredentialInMemoryStore : CredentialStoring {
    private val secrets = mutableMapOf<String, String>()

    override fun secret(forProviderId: String): String? =
        secrets[forProviderId]?.takeIf { it.isNotEmpty() }

    override fun save(secret: String, forProviderId: String) {
        secrets[forProviderId] = secret
    }

    override fun clear(forProviderId: String) {
        secrets.remove(forProviderId)
    }
}
