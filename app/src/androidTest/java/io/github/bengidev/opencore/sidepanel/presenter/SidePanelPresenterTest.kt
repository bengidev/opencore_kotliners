package io.github.bengidev.opencore.sidepanel.presenter

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.shared.credential.CredentialInMemoryStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SidePanelPresenterTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsGear_showsProviderPickerAndApiKeyField() {
        val component = createSidePanelComponent()
        composeRule.setContent {
            OpenCoreHomeTheme(darkTheme = false) {
                SidePanelView(component = component)
            }
        }

        component.toggleSidebar()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)

        composeRule.onNodeWithTag("sidepanel-settings-button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings-provider-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-api-key-field").assertIsDisplayed()
    }

    @Test
    fun saveApiKey_showsStoredIndicator() {
        val component = createSidePanelComponent()
        composeRule.setContent {
            OpenCoreHomeTheme(darkTheme = false) {
                SidePanelView(component = component)
            }
        }

        component.toggleSidebar()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag("sidepanel-settings-button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings-api-key-field").performTextInput("sk-test")
        composeRule.onNodeWithTag("settings-save-button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings-key-stored").assertIsDisplayed()
    }

    @Test
    fun longPressConversationRow_showsActionDialog() {
        val component = createSidePanelComponent(
            history = InMemorySidePanelHistoryRepository(seed = SidePanelConversation.previewSamples())
        )
        composeRule.setContent {
            OpenCoreHomeTheme(darkTheme = false) {
                SidePanelView(component = component)
            }
        }

        component.toggleSidebar()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)

        composeRule.onAllNodesWithTag("history-conversation-row")
            .onFirst()
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Rename").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    private fun createSidePanelComponent(
        history: InMemorySidePanelHistoryRepository = InMemorySidePanelHistoryRepository(seed = emptyList())
    ): SidePanelComponent {
        val lifecycle = LifecycleRegistry().apply { resume() }
        return SidePanelComponent(
            componentContext = DefaultComponentContext(lifecycle),
            history = history,
            credentialStore = CredentialInMemoryStore(),
            preferenceStore = InMemorySidePanelPreferenceStore()
        )
    }
}
