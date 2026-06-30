package io.github.bengidev.opencore.speech.utilities

import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.infrastructure.SpeechSystemRecognitionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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

        fun live(engineFactory: () -> SpeechSystemRecognitionEngine): SpeechRecognitionClient {
            val session = SpeechRecognitionSession(engineFactory)
            return object : SpeechRecognitionClient {
                override fun authorizationStatus(): SpeechAuthorizationStatus =
                    session.engineFactory().authorizationStatus()

                override suspend fun requestAuthorization(): SpeechAuthorizationStatus =
                    session.engineFactory().requestAuthorization()

                override fun start(): Flow<SpeechRecognitionEvent> = session.start()

                override suspend fun stop(): SpeechRecognitionResult? = session.stop()
            }
        }
    }
}

private class SpeechRecognitionSession(
    val engineFactory: () -> SpeechSystemRecognitionEngine,
) {
    private var engine: SpeechSystemRecognitionEngine? = null

    fun start(): Flow<SpeechRecognitionEvent> {
        val active = engine ?: engineFactory().also { engine = it }
        return active.start()
    }

    suspend fun stop(): SpeechRecognitionResult? {
        val active = engine ?: return null
        engine = null
        return active.stop()
    }
}
