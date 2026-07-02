package io.github.bengidev.opencore.speech.application

import android.content.Context
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import io.github.bengidev.opencore.speech.utilities.SpeechRecordingLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpeechFlowControllerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsIdleWithoutListeningOrTranscript() {
        val controller = makeController(harness = SpeechRecognitionTestHarness())

        assertFalse(controller.state.value.isListening)
        assertTrue(controller.state.value.partialTranscript.isEmpty())
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun startsListeningWhenAuthorized() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        assertTrue(controller.state.value.isListening)
        assertEquals(1, harness.startCallCount)

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun updatesPartialTranscript() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready, SpeechRecognitionEvent.Partial("hello"))
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        assertEquals("hello", controller.state.value.partialTranscript)
        assertEquals("typed text", controller.displayedDraft(base = "typed text"))

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun deniedAuthorizationShowsError() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.authorizationStatus = SpeechAuthorizationStatus.DENIED
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(1)

        assertFalse(controller.state.value.isListening)
        assertNotNull(controller.state.value.errorMessage)
        assertEquals(0, harness.startCallCount)
    }

    @Test
    fun cancelListeningDiscardsTranscriptAndAudio() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        val tempAudio = File.createTempFile("voice-note", ".wav", context.cacheDir)
        tempAudio.writeBytes(byteArrayOf(0x00, 0x01))
        harness.events = listOf(SpeechRecognitionEvent.Ready, SpeechRecognitionEvent.Partial("discard me"))
        harness.stopResult = SpeechRecognitionResult(
            transcript = "discard me",
            audioFilePath = tempAudio.absolutePath,
        )
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)
        controller.cancelListening()
        advanceTimeBy(1)

        assertFalse(controller.state.value.isListening)
        assertTrue(controller.state.value.partialTranscript.isEmpty())
        assertFalse(tempAudio.exists())
    }

    @Test
    fun audioLevelEventsUpdatePresentation() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(
            SpeechRecognitionEvent.Ready,
            SpeechRecognitionEvent.AudioLevel(0.001f),
            SpeechRecognitionEvent.AudioLevel(0.05f),
        )
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(200)

        assertTrue(controller.state.value.isVoiceActive)
        assertEquals(2, controller.state.value.audioLevels.size)

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun concurrentStartOnlyBeginsOnce() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = makeController(harness)

        launch { controller.startListening() }
        launch { controller.startListening() }
        advanceTimeBy(50)

        assertEquals(1, harness.startCallCount)

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun stopListeningReturnsTranscriptForComposer() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        val tempAudio = File.createTempFile("voice-note", ".wav", context.cacheDir)
        tempAudio.writeBytes(byteArrayOf(0x00, 0x01))
        harness.stopResult = SpeechRecognitionResult(
            transcript = "send this",
            audioFilePath = tempAudio.absolutePath,
            durationSeconds = 2.0,
        )
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        val capture = controller.stopListening()
        advanceTimeBy(1)

        assertEquals("send this", capture?.composerText)
        assertFalse(controller.state.value.isListening)
        assertEquals(1, harness.stopCallCount)
        assertFalse(tempAudio.exists())
    }

    @Test
    fun stopListeningShowsErrorWhenNoSpeechDetected() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(transcript = "")
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        val capture = controller.stopListening()
        advanceTimeBy(1)

        assertNull(capture)
        assertEquals(
            "No speech was detected. Try again or type your message.",
            controller.state.value.errorMessage,
        )
    }

    @Test
    fun stopListeningIgnoresFailedEventDuringIntentionalStop() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(transcript = "")
        harness.failedOnStop = SpeechRecognitionEvent.Failed("No speech was recognized.")
        harness.events = listOf(
            SpeechRecognitionEvent.Ready,
            SpeechRecognitionEvent.Partial("hello there"),
        )
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        val capture = controller.stopListening()
        advanceTimeBy(1)

        assertEquals("hello there", capture?.composerText)
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun failedEventStillSurfacesWhenNotStopping() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = false
        harness.events = listOf(
            SpeechRecognitionEvent.Ready,
            SpeechRecognitionEvent.Failed("Recognizer unavailable"),
        )
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        assertEquals("Recognizer unavailable", controller.state.value.errorMessage)
        assertFalse(controller.state.value.isListening)
    }

    @Test
    fun stopListeningUsesPartialTranscriptWhenStopReturnsEmpty() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(transcript = "")
        harness.events = listOf(
            SpeechRecognitionEvent.Ready,
            SpeechRecognitionEvent.Partial("hello there"),
        )
        val controller = makeController(harness)

        controller.startListening()
        advanceTimeBy(50)

        val capture = controller.stopListening()
        advanceTimeBy(1)

        assertEquals("hello there", capture?.composerText)
    }

    @Test
    fun autoStopTriggersAtThreshold() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(transcript = "timed out")
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = makeController(
            harness = harness,
            autoStopThresholdSeconds = 1.0,
        )

        controller.startListening()
        advanceTimeBy(50)
        advanceTimeBy(1_100)

        assertEquals(1, harness.stopCallCount)
        assertFalse(controller.state.value.isListening)
        assertEquals("timed out", controller.state.value.pendingCapture?.composerText)
    }

    @Test
    fun mergedDraftSpacing() {
        assertEquals("Hi there", SpeechFlowController.mergedDraft("Hi", "there"))
        assertEquals("Hi there", SpeechFlowController.mergedDraft("Hi ", "there"))
        assertEquals("there", SpeechFlowController.mergedDraft("", "there"))
    }

    private fun makeController(
        harness: SpeechRecognitionTestHarness,
        autoStopThresholdSeconds: Double = SpeechRecordingLimits.autoStopThresholdSeconds,
    ): SpeechFlowController = SpeechFlowController(
        recognitionFactory = { harness.makeClient() },
        scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
        context = context,
        autoStopThresholdSeconds = autoStopThresholdSeconds,
        microphoneAuthorizationStatus = { SpeechAuthorizationStatus.AUTHORIZED },
        requestMicrophoneAuthorization = { SpeechAuthorizationStatus.AUTHORIZED },
    )
}

private class SpeechRecognitionTestHarness {
    var authorizationStatus: SpeechAuthorizationStatus = SpeechAuthorizationStatus.AUTHORIZED
    var events: List<SpeechRecognitionEvent> = emptyList()
    var hangsOpenAfterEvents: Boolean = false
    var stopResult: SpeechRecognitionResult? = null
    var failedOnStop: SpeechRecognitionEvent.Failed? = null
    var startCallCount: Int = 0
        private set
    var stopCallCount: Int = 0
        private set

    private var emitDuringSession: ((SpeechRecognitionEvent) -> Unit)? = null

    fun emit(event: SpeechRecognitionEvent) {
        emitDuringSession?.invoke(event)
    }

    fun makeClient(): SpeechRecognitionClient = object : SpeechRecognitionClient {
        override fun authorizationStatus(): SpeechAuthorizationStatus = this@SpeechRecognitionTestHarness.authorizationStatus

        override suspend fun requestAuthorization(): SpeechAuthorizationStatus =
            this@SpeechRecognitionTestHarness.authorizationStatus

        override fun start(): Flow<SpeechRecognitionEvent> = callbackFlow {
            startCallCount += 1
            emitDuringSession = { event -> trySend(event) }
            events.forEach { trySend(it) }
            if (hangsOpenAfterEvents) {
                awaitClose { emitDuringSession = null }
            } else {
                close()
                emitDuringSession = null
            }
        }

        override suspend fun stop(): SpeechRecognitionResult? {
            stopCallCount += 1
            failedOnStop?.let { emitDuringSession?.invoke(it) }
            return stopResult
        }
    }
}
