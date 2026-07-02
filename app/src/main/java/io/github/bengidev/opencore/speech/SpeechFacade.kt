package io.github.bengidev.opencore.speech

import android.content.Context
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.speech.application.SpeechFlowController
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

internal class SpeechFacade {
    fun createController(
        context: Context,
        scope: CoroutineScope,
        permissionRequester: suspend () -> Boolean,
        credentialStore: CredentialStoring,
        preferenceProvider: suspend () -> SidePanelProviderPreference,
        locale: Locale = Locale.getDefault(),
    ): SpeechFlowController {
        val recognitionFactory = SpeechRecognitionClient.live(
            context = context,
            scope = scope,
            locale = locale,
            permissionRequester = permissionRequester,
            credentialStore = credentialStore,
            preferenceProvider = preferenceProvider,
        )
        return SpeechFlowController(
            recognitionFactory = recognitionFactory,
            scope = scope,
        )
    }
}
