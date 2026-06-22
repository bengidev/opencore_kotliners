package io.github.bengidev.opencore.sidepanel.infrastructure

internal interface SidePanelCredentialStore {
    fun secret(forProviderId: String): String?
    fun save(secret: String, forProviderId: String)
    fun clear(forProviderId: String)
}
