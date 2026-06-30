package io.github.bengidev.opencore.speech

import android.content.Context
import io.github.bengidev.opencore.speech.application.SpeechFlowController
import io.github.bengidev.opencore.speech.infrastructure.SpeechSystemRecognitionEngine
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

internal class SpeechFacade {
    fun createController(
        context: Context,
        scope: CoroutineScope,
        permissionRequester: suspend () -> Boolean,
        locale: Locale = Locale.getDefault(),
    ): SpeechFlowController {
        val recognition = SpeechRecognitionClient.live {
            SpeechSystemRecognitionEngine(
                context = context,
                locale = locale,
                permissionRequester = permissionRequester,
            )
        }
        return SpeechFlowController(recognition = recognition, scope = scope, context = context)
    }
}
