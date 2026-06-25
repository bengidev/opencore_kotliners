package io.github.bengidev.opencore.shared.credential

/** In-memory [CredentialStoring] test double. */
internal class CredentialInMemoryStore : CredentialStoring {
    private val secrets = mutableMapOf<String, String>()

    override fun secret(providerId: String): String? =
        secrets[providerId]?.takeIf { it.isNotEmpty() }

    override fun save(secret: String, providerId: String) {
        secrets[providerId] = secret
    }

    override fun clear(providerId: String) {
        secrets.remove(providerId)
    }
}
