package io.github.bengidev.opencore.shared.credential

/** Proxy contract for opaque provider secrets. */
internal interface CredentialStoring {
    fun secret(providerId: String): String?
    fun save(secret: String, providerId: String)
    fun clear(providerId: String)
}
