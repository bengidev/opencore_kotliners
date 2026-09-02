package io.github.bengidev.opencore.onboarding.presenter.chat

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bengidev.opencore.onboarding.domain.OnboardingChatMessage
import io.github.bengidev.opencore.onboarding.domain.OnboardingFeature
import io.github.bengidev.opencore.onboarding.theme.OpenCoreOnboardingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingChatPresenterTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reduceMotionFeed_showsUserAndAssistantWithoutThinking() {
        val feature = OnboardingFeature.catalog.first()

        composeRule.setContent {
            OpenCoreOnboardingTheme(darkTheme = false) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(480.dp)
                ) {
                    OnboardingFeatureChatFeedView(
                        feedActive = true,
                        reduceMotion = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(feature.userPrompt).assertIsDisplayed()
        composeRule.onNodeWithText(feature.title).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Thinking…").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun chatBubble_rendersUserThinkingAndAssistantRoles() {
        val feature = OnboardingFeature.catalog.first()

        composeRule.setContent {
            OpenCoreOnboardingTheme(darkTheme = false) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(480.dp)
                ) {
                    OnboardingChatBubbleView(
                        message = OnboardingChatMessage.user(feature.userPrompt, feature),
                        containerWidth = maxWidth,
                        reduceMotion = true
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(feature.userPrompt).assertIsDisplayed()

        composeRule.setContent {
            OpenCoreOnboardingTheme(darkTheme = false) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(480.dp)
                ) {
                    OnboardingChatBubbleView(
                        message = OnboardingChatMessage.thinking(feature),
                        containerWidth = maxWidth,
                        reduceMotion = true
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Thinking…").assertIsDisplayed()

        composeRule.setContent {
            OpenCoreOnboardingTheme(darkTheme = false) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(480.dp)
                ) {
                    OnboardingChatBubbleView(
                        message = OnboardingChatMessage.assistant(feature),
                        containerWidth = maxWidth,
                        reduceMotion = true
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(feature.title).assertIsDisplayed()
        composeRule.onNodeWithText(feature.subtitle).assertIsDisplayed()
    }
}
