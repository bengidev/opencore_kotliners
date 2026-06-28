package io.github.bengidev.opencore.chat.presenter

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatThreadViewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shortThread_rendersMessagesAboveComposerArea() {
        val state = ChatState(
            messages = listOf(
                testMessage(role = ChatMessageRole.USER, content = "Hi there"),
                testMessage(role = ChatMessageRole.ASSISTANT, content = "Hello back"),
            ),
        )

        composeRule.setContent {
            OpenCoreHomeTheme(darkTheme = false) {
                ChatThreadView(
                    state = state,
                    modifier = Modifier
                        .fillMaxHeight()
                        .height(480.dp),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-thread-list").assertIsDisplayed()
        composeRule.onNodeWithText("Hi there").assertIsDisplayed()
        composeRule.onNodeWithText("Hello back").assertIsDisplayed()
    }

    private fun testMessage(role: String, content: String): SidePanelMessage =
        SidePanelMessage(
            id = UUID.randomUUID(),
            role = role,
            content = content,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            kind = SidePanelMessageKind.TEXT,
        )
}
