package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatibleStreamingClientTest {

  @Test
  fun stream_emitsFromChunkCallbackWithoutFlowInvariantViolation() = runTest {
    val payload =
      """data: {"choices":[{"delta":{"content":"Hello"}}]}""" + "\n\n" +
        "data: [DONE]\n\n"
    val client = OpenAiCompatibleStreamingClient { _, _, _, onChunk ->
      withContext(Dispatchers.IO) {
        onChunk(payload.toByteArray())
      }
      HttpStreamResult.Success
    }

    val events = client
      .stream(
        provider = SidePanelProviderApi.openRouter,
        modelId = "test-model",
        apiKey = "test-key",
        messages = emptyList(),
        reasoning = SidePanelReasoningModel.Off
      )
      .toList()

    assertEquals(ChatStreamingEvent.TextDelta("Hello"), events[0])
    assertEquals(ChatStreamingEvent.Done, events[1])
  }

  @Test
  fun stream_httpFailure_emitsError() = runTest {
    val client = OpenAiCompatibleStreamingClient { _, _, _, _ ->
      HttpStreamResult.Failure(401, """{"error":{"message":"Invalid key"}}""")
    }

    val events = client
      .stream(
        provider = SidePanelProviderApi.openRouter,
        modelId = "test-model",
        apiKey = "bad-key",
        messages = emptyList(),
        reasoning = SidePanelReasoningModel.Off
      )
      .toList()

    assertTrue(events.first() is ChatStreamingEvent.Error)
    assertTrue((events.first() as ChatStreamingEvent.Error).error.message.contains("401"))
  }
}
