package io.github.bengidev.opencore.speech.infrastructure

import io.github.bengidev.opencore.shared.credential.CredentialInMemoryStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpeechWhisperTranscriberTest {
    @Test
    fun buildMultipartBody_includesModelAndFileParts() {
        val transcriber = SpeechWhisperTranscriber(
            credentialStore = CredentialInMemoryStore(),
            contextResolver = { null },
        )
        val body = transcriber.buildMultipartBody(
            boundary = "test-boundary",
            model = "whisper-1",
            filename = "audio.wav",
            mimeType = "audio/wav",
            audioData = byteArrayOf(0x01, 0x02),
        )

        val text = body.decodeToString()
        assertTrue(text.contains("name=\"model\""))
        assertTrue(text.contains("whisper-1"))
        assertTrue(text.contains("filename=\"audio.wav\""))
        assertTrue(text.contains("Content-Type: audio/wav"))
        assertTrue(text.endsWith("--test-boundary--\r\n"))
    }

    @Test
    fun openRouterJsonBody_usesInputAudioShape() {
        val body = buildOpenRouterJsonBody(
            model = "openai/whisper-1",
            audioFormat = "wav",
            audioData = byteArrayOf(0x01, 0x02, 0x03),
        )

        val json = JSONObject(body.decodeToString())
        assertEquals("openai/whisper-1", json.getString("model"))
        val inputAudio = json.getJSONObject("input_audio")
        assertEquals("wav", inputAudio.getString("format"))
        assertEquals("AQID", inputAudio.getString("data"))
    }
}

private fun buildOpenRouterJsonBody(
    model: String,
    audioFormat: String,
    audioData: ByteArray,
): ByteArray {
    return JSONObject().apply {
        put("model", model)
        put(
            "input_audio",
            JSONObject().apply {
                put("data", java.util.Base64.getEncoder().encodeToString(audioData))
                put("format", audioFormat)
            },
        )
    }.toString().toByteArray(Charsets.UTF_8)
}
