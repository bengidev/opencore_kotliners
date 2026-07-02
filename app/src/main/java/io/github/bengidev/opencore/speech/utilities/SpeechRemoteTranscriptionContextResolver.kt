package io.github.bengidev.opencore.speech.utilities

import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.speech.domain.SpeechRemoteTranscriptionContext

/** Resolves the active chat provider into a remote transcription context. */
internal object SpeechRemoteTranscriptionContextResolver {
    fun make(
        credentialStore: CredentialStoring,
        preferenceProvider: suspend () -> SidePanelProviderPreference,
    ): suspend () -> SpeechRemoteTranscriptionContext? = {
        val preference = preferenceProvider()
        val adapter = ProviderRegistry.resolve(preference.providerId)
        val descriptor = adapter.descriptor
        if (credentialStore.secret(descriptor.id).isNullOrBlank() || !descriptor.supportsAudioTranscription) {
            null
        } else {
            SpeechRemoteTranscriptionContext(
                providerId = descriptor.id,
                apiBaseUrl = descriptor.baseUrl,
                defaultHeaders = descriptor.defaultHeaders,
            )
        }
    }
}
