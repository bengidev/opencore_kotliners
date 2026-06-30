package io.github.bengidev.opencore.speech.application

import android.content.Context
import io.github.bengidev.opencore.speech.domain.SpeechAuthorizationStatus
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionEvent
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.utilities.SpeechRecognitionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        val controller = SpeechFlowController(
            recognition = SpeechRecognitionClient.preview,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            context = context,
        )

        assertFalse(controller.state.value.isListening)
        assertTrue(controller.state.value.partialTranscript.isEmpty())
        assertNull(controller.state.value.errorMessage)
    }

    @Test
    fun startsListeningWhenAuthorized() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

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
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(50)

        assertEquals("hello", controller.state.value.partialTranscript)

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun deniedAuthorizationShowsError() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.authorizationStatus = SpeechAuthorizationStatus.DENIED
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(1)

        assertFalse(controller.state.value.isListening)
        assertNotNull(controller.state.value.errorMessage)
        assertEquals(0, harness.startCallCount)
    }

    @Test
    fun cancelListeningDiscardsTranscript() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready, SpeechRecognitionEvent.Partial("discard me"))
        harness.stopResult = SpeechRecognitionResult(transcript = "discard me")
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(50)
        assertEquals("discard me", controller.state.value.partialTranscript)

        controller.cancelListening()
        advanceTimeBy(1)

        assertFalse(controller.state.value.isListening)
        assertTrue(controller.state.value.partialTranscript.isEmpty())
        assertTrue(controller.state.value.audioLevels.isEmpty())
        assertFalse(controller.state.value.isVoiceActive)
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
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(200)

        assertTrue(controller.state.value.isVoiceActive)
        assertEquals(2, controller.state.value.audioLevels.size)
        assertEquals(0.05f, controller.state.value.audioLevels.last())

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun concurrentStartOnlyBeginsOnce() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        launch { controller.startListening() }
        launch { controller.startListening() }
        advanceTimeBy(50)

        assertEquals(1, harness.startCallCount)

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun displayedDraftMergesPartialTranscript() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.events = listOf(SpeechRecognitionEvent.Ready, SpeechRecognitionEvent.Partial("world"))
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(50)

        assertEquals("Hello world", controller.displayedDraft(base = "Hello"))

        controller.cancelListening()
        advanceTimeBy(1)
    }

    @Test
    fun stopListeningWithoutAudioReturnsNullAttachment() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(transcript = "there")
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(50)

        val attachment = controller.stopListening()
        advanceTimeBy(1)

        assertNull(attachment)
        assertFalse(controller.state.value.isListening)
    }

    @Test
    fun stopListeningShowsErrorWhenAudioCapturedWithoutTranscript() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(
            transcript = "",
            audioFilePath = "/tmp/voice-note.wav",
        )
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(50)

        val attachment = controller.stopListening()
        advanceTimeBy(1)

        assertNull(attachment)
        assertEquals(
            "Voice note could not be transcribed. Try again or type your message.",
            controller.state.value.errorMessage,
        )
    }

    @Test
    fun stopListeningCreatesAttachmentFromTranscriptWithSilentAudioFallback() = runTest(testDispatcher) {
        val harness = SpeechRecognitionTestHarness()
        harness.hangsOpenAfterEvents = true
        harness.stopResult = SpeechRecognitionResult(
            transcript = "hello there",
            durationSeconds = 1.0,
        )
        harness.events = listOf(SpeechRecognitionEvent.Ready)
        val controller = SpeechFlowController(
            recognition = harness.makeClient(),
            scope = backgroundScope,
            context = context,
        )

        controller.startListening()
        advanceTimeBy(300)

        val attachment = controller.stopListening()
        advanceTimeBy(1)

        assertNotNull(attachment)
        assertEquals("hello there", attachment?.speechTranscript)
        assertTrue(attachment?.localPath?.endsWith(".wav") == true)
    }

    @Test
    fun mergedDraftSpacing() {
        assertEquals("Hi there", SpeechFlowController.mergedDraft("Hi", "there"))
        assertEquals("Hi there", SpeechFlowController.mergedDraft("Hi ", "there"))
        assertEquals("there", SpeechFlowController.mergedDraft("", "there"))
    }
}

private class SpeechRecognitionTestHarness {
    var authorizationStatus: SpeechAuthorizationStatus = SpeechAuthorizationStatus.AUTHORIZED
    var events: List<SpeechRecognitionEvent> = emptyList()
    var hangsOpenAfterEvents: Boolean = false
    var stopResult: SpeechRecognitionResult? = null
    var startCallCount: Int = 0
        private set
    var stopCallCount: Int = 0
        private set

    fun makeClient(): SpeechRecognitionClient = object : SpeechRecognitionClient {
        override fun authorizationStatus(): SpeechAuthorizationStatus = this@SpeechRecognitionTestHarness.authorizationStatus

        override suspend fun requestAuthorization(): SpeechAuthorizationStatus =
            this@SpeechRecognitionTestHarness.authorizationStatus

        override fun start(): Flow<SpeechRecognitionEvent> = flow {
            startCallCount += 1
            events.forEach { emit(it) }
            if (hangsOpenAfterEvents) {
                delay(Long.MAX_VALUE)
            }
        }

        override suspend fun stop(): SpeechRecognitionResult? {
            stopCallCount += 1
            return stopResult
        }
    }
}
