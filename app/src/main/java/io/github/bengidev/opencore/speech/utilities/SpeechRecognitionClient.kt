package io.github.bengidev.opencore.speech.utilities

import android.content.Context
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.infrastructure.SpeechContinuousCaptureEngine
import io.github.bengidev.opencore.speech.infrastructure.SpeechSystemRecognitionEngine
import io.github.bengidev.opencore.speech.infrastructure.SpeechWhisperTranscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Locale

/** Injectable boundary for on-device speech recognition. */
internal interface SpeechRecognitionClient {
    fun authorizationStatus(): SpeechAuthorizationStatus
    suspend fun requestAuthorization(): SpeechAuthorizationStatus
    fun start(): Flow<SpeechRecognitionEvent>
    suspend fun stop(): SpeechRecognitionResult?

    companion object {
        val preview: SpeechRecognitionClient = object : SpeechRecognitionClient {
            override fun authorizationStatus(): SpeechAuthorizationStatus = SpeechAuthorizationStatus.AUTHORIZED
            override suspend fun requestAuthorization(): SpeechAuthorizationStatus = SpeechAuthorizationStatus.AUTHORIZED
            override fun start(): Flow<SpeechRecognitionEvent> = emptyFlow()
            override suspend fun stop(): SpeechRecognitionResult? = null
        }

        fun live(
            context: Context,
            scope: CoroutineScope,
            locale: Locale,
            permissionRequester: suspend () -> Boolean,
            credentialStore: CredentialStoring,
            preferenceProvider: suspend () -> SidePanelProviderPreference,
        ): suspend () -> SpeechRecognitionClient {
            val captureClient = CaptureRecognitionClient(
                engine = SpeechContinuousCaptureEngine(context),
                whisperTranscriber = SpeechWhisperTranscriber(
                    credentialStore = credentialStore,
                    contextResolver = SpeechRemoteTranscriptionContextResolver.make(
                        credentialStore = credentialStore,
                        preferenceProvider = preferenceProvider,
                    ),
                ),
                scope = scope,
                permissionRequester = permissionRequester,
            )
            val systemClient = SystemRecognitionClient {
                SpeechSystemRecognitionEngine(
                    context = context,
                    locale = locale,
                    permissionRequester = permissionRequester,
                )
            }
            val whisperTranscriber = captureClient.whisperTranscriberForSelection()
            return {
                if (whisperTranscriber.hasCredential()) captureClient else systemClient
            }
        }
    }
}

private class CaptureRecognitionClient(
    private val engine: SpeechContinuousCaptureEngine,
    private val whisperTranscriber: SpeechWhisperTranscriber,
    private val scope: CoroutineScope,
    private val permissionRequester: suspend () -> Boolean,
) : SpeechRecognitionClient {
    fun whisperTranscriberForSelection(): SpeechWhisperTranscriber = whisperTranscriber

    override fun authorizationStatus(): SpeechAuthorizationStatus = engine.authorizationStatus()

    override suspend fun requestAuthorization(): SpeechAuthorizationStatus =
        engine.requestAuthorization(permissionRequester)

    override fun start(): Flow<SpeechRecognitionEvent> = engine.start(scope)

    override suspend fun stop(): SpeechRecognitionResult? {
        val captured = engine.stop() ?: return null
        val transcript = captured.transcript.trim()
        if (transcript.isNotEmpty()) return captured

        val audioPath = captured.audioFilePath ?: return captured
        return whisperTranscriber.transcribe(
            audioFilePath = audioPath,
            durationSeconds = captured.durationSeconds,
        ) ?: captured
    }
}

private class SystemRecognitionClient(
    private val engineFactory: () -> SpeechSystemRecognitionEngine,
) : SpeechRecognitionClient {
    private val session = SpeechRecognitionSession(engineFactory)

    override fun authorizationStatus(): SpeechAuthorizationStatus =
        session.engineFactory().authorizationStatus()

    override suspend fun requestAuthorization(): SpeechAuthorizationStatus =
        session.engineFactory().requestAuthorization()

    override fun start(): Flow<SpeechRecognitionEvent> = session.start()

    override suspend fun stop(): SpeechRecognitionResult? = session.stop()
}

private class SpeechRecognitionSession(
    val engineFactory: () -> SpeechSystemRecognitionEngine,
) {
    private var engine: SpeechSystemRecognitionEngine? = null
    private var lastStopResult: SpeechRecognitionResult? = null

    fun start(): Flow<SpeechRecognitionEvent> {
        lastStopResult = null
        val active = engine ?: engineFactory().also { engine = it }
        return active.start()
    }

    suspend fun stop(): SpeechRecognitionResult? {
        lastStopResult?.let { return it }
        val active = engine ?: return null
        engine = null
        val result = active.stop()
        lastStopResult = result
        return result
    }
}
