package io.github.bengidev.opencore.speech.infrastructure

import android.util.Base64
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.speech.domain.SpeechRecognitionResult
import io.github.bengidev.opencore.speech.domain.SpeechRemoteTranscriptionContext
import io.github.bengidev.opencore.speech.utilities.SpeechWhisperUploadPreparer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID

/** Remote speech transcription via the active provider's speech-to-text API. */
internal class SpeechWhisperTranscriber(
    private val credentialStore: CredentialStoring,
    private val contextResolver: suspend () -> SpeechRemoteTranscriptionContext?,
    private val openAiModel: String = "whisper-1",
    private val openRouterModel: String = "openai/whisper-1",
) {
    suspend fun hasCredential(): Boolean = contextResolver() != null

    suspend fun transcribe(
        audioFilePath: String,
        durationSeconds: Double,
    ): SpeechRecognitionResult? = withContext(Dispatchers.IO) {
        var activeConnection: HttpURLConnection? = null
        coroutineContext[Job]?.invokeOnCompletion { activeConnection?.disconnect() }

        val audioFile = File(audioFilePath)
        val context = contextResolver()
            ?: return@withContext missingCredentialResult(audioFile, durationSeconds)
        val apiKey = credentialStore.secret(context.providerId)
            ?: return@withContext missingCredentialResult(audioFile, durationSeconds)

        val preparedUpload = runCatching {
            SpeechWhisperUploadPreparer.prepareUpload(audioFile)
        }.getOrElse {
            return@withContext SpeechRecognitionResult(
                transcript = "",
                audioFilePath = audioFilePath,
                durationSeconds = durationSeconds,
                failureMessage = "Voice recording could not be prepared for transcription.",
            )
        }

        try {
            when (
                val outcome = uploadForTranscription(
                    preparedUpload = preparedUpload,
                    apiKey = apiKey,
                    context = context,
                    registerConnection = { activeConnection = it },
                )
            ) {
                is TranscriptionOutcome.Success -> SpeechRecognitionResult(
                    transcript = outcome.transcript,
                    audioFilePath = audioFilePath,
                    durationSeconds = durationSeconds,
                )
                is TranscriptionOutcome.Failure -> SpeechRecognitionResult(
                    transcript = "",
                    audioFilePath = audioFilePath,
                    durationSeconds = durationSeconds,
                    failureMessage = outcome.message,
                )
            }
        } finally {
            if (preparedUpload.shouldDeleteAfterUpload) {
                preparedUpload.file.delete()
            }
        }
    }

    private fun missingCredentialResult(
        audioFile: File,
        durationSeconds: Double,
    ): SpeechRecognitionResult = SpeechRecognitionResult(
        transcript = "",
        audioFilePath = audioFile.takeIf { it.exists() }?.absolutePath,
        durationSeconds = durationSeconds,
        failureMessage = "Add an API key in Settings to transcribe voice input.",
    )

    private fun uploadForTranscription(
        preparedUpload: SpeechWhisperUploadPreparer.PreparedUpload,
        apiKey: String,
        context: SpeechRemoteTranscriptionContext,
        registerConnection: (HttpURLConnection) -> Unit,
    ): TranscriptionOutcome {
        return if (context.providerId == OPENROUTER_PROVIDER_ID) {
            uploadOpenRouterJson(preparedUpload, apiKey, context, registerConnection)
        } else {
            uploadOpenAiMultipart(preparedUpload, apiKey, context, registerConnection)
        }
    }

    private fun uploadOpenRouterJson(
        preparedUpload: SpeechWhisperUploadPreparer.PreparedUpload,
        apiKey: String,
        context: SpeechRemoteTranscriptionContext,
        registerConnection: (HttpURLConnection) -> Unit,
    ): TranscriptionOutcome {
        val audioData = preparedUpload.file.readBytes()
        if (audioData.isEmpty()) {
            return TranscriptionOutcome.Failure("Voice recording could not be read.")
        }

        val audioFormat = preparedUpload.file.extension.lowercase().ifBlank { "wav" }
        val body = JSONObject().apply {
            put("model", openRouterModel)
            put(
                "input_audio",
                JSONObject().apply {
                    put("data", Base64.encodeToString(audioData, Base64.NO_WRAP))
                    put("format", audioFormat)
                },
            )
        }.toString().toByteArray(Charsets.UTF_8)

        val connection = openConnection(context, apiKey).apply {
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setFixedLengthStreamingMode(body.size)
        }

        return executeTranscriptionRequest(connection, body, registerConnection)
    }

    private fun uploadOpenAiMultipart(
        preparedUpload: SpeechWhisperUploadPreparer.PreparedUpload,
        apiKey: String,
        context: SpeechRemoteTranscriptionContext,
        registerConnection: (HttpURLConnection) -> Unit,
    ): TranscriptionOutcome {
        val audioData = preparedUpload.file.readBytes()
        if (audioData.isEmpty()) {
            return TranscriptionOutcome.Failure("Voice recording could not be read.")
        }

        val boundary = UUID.randomUUID().toString()
        val body = buildMultipartBody(
            boundary = boundary,
            model = openAiModel,
            filename = preparedUpload.filename,
            mimeType = preparedUpload.mimeType,
            audioData = audioData,
        )

        val connection = openConnection(context, apiKey).apply {
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setFixedLengthStreamingMode(body.size)
        }

        return executeTranscriptionRequest(connection, body, registerConnection)
    }

    private fun openConnection(
        context: SpeechRemoteTranscriptionContext,
        apiKey: String,
    ): HttpURLConnection = (URL(context.audioTranscriptionsUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        useCaches = false
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("Accept", "application/json")
        context.defaultHeaders.forEach { (key, value) ->
            setRequestProperty(key, value)
        }
    }

    private fun executeTranscriptionRequest(
        connection: HttpURLConnection,
        body: ByteArray,
        registerConnection: (HttpURLConnection) -> Unit,
    ): TranscriptionOutcome {
        registerConnection(connection)
        return try {
            connection.outputStream.use { output ->
                output.write(body)
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText().orEmpty()
            }

            if (responseCode !in 200..299) {
                return TranscriptionOutcome.Failure(
                    message = formatHttpFailure(responseCode, responseBody),
                )
            }

            val transcript = runCatching {
                JSONObject(responseBody).optString("text").trim()
            }.getOrDefault("")
            if (transcript.isBlank()) {
                TranscriptionOutcome.Failure("Speech recognition returned an empty transcript.")
            } else {
                TranscriptionOutcome.Success(transcript)
            }
        } catch (error: SocketTimeoutException) {
            TranscriptionOutcome.Failure("Voice transcription timed out. Try again.")
        } catch (error: Exception) {
            TranscriptionOutcome.Failure(
                message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Speech recognition could not reach the server.",
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun buildMultipartBody(
        boundary: String,
        model: String,
        filename: String,
        mimeType: String,
        audioData: ByteArray,
    ): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            writeFormField(stream, boundary, "model", model)
            writeFileField(
                output = stream,
                boundary = boundary,
                fieldName = "file",
                filename = filename,
                mimeType = mimeType,
                data = audioData,
            )
            stream.writeBytes("--$boundary--\r\n")
        }
        return output.toByteArray()
    }

    private fun writeFormField(
        output: DataOutputStream,
        boundary: String,
        name: String,
        value: String,
    ) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.writeBytes("$value\r\n")
    }

    private fun writeFileField(
        output: DataOutputStream,
        boundary: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        data: ByteArray,
    ) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes(
            "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$filename\"\r\n",
        )
        output.writeBytes("Content-Type: $mimeType\r\n\r\n")
        output.write(data)
        output.writeBytes("\r\n")
    }

    private fun formatHttpFailure(responseCode: Int, responseBody: String): String {
        val detail = runCatching {
            JSONObject(responseBody).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return when (responseCode) {
            401, 403 -> "Voice transcription failed. Check your API key in Settings."
            413 -> "Voice recording is too large to transcribe."
            429 -> "Voice transcription is rate limited. Try again shortly."
            else -> detail ?: "Voice transcription failed (HTTP $responseCode)."
        }
    }

    private sealed class TranscriptionOutcome {
        data class Success(val transcript: String) : TranscriptionOutcome()
        data class Failure(val message: String) : TranscriptionOutcome()
    }

    companion object {
        private const val OPENROUTER_PROVIDER_ID = "openrouter"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
