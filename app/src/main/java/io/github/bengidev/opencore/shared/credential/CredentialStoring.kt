package io.github.bengidev.opencore.shared.credential

/** Proxy contract for opaque provider secrets. */
internal interface CredentialStoring {
    fun secret(forProviderId: String): String?
    fun save(secret: String, forProviderId: String)
    fun clear(forProviderId: String)
}
