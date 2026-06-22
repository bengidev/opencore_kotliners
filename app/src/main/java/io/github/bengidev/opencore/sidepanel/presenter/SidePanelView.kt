package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent

@Composable
internal fun SidePanelView(
    component: SidePanelComponent,
    modifier: Modifier = Modifier
) {
    val sessionState by component.sessionState.subscribeAsState()
    val showSettings by component.showSettings.subscribeAsState()

    Box(modifier = modifier.fillMaxSize()) {
        SidePanelSessionDrawer(
            component = component.session,
            state = sessionState
        )

        if (showSettings) {
            component.setting?.let { settingComponent ->
                SidePanelSettingSheet(
                    component = settingComponent,
                    onDismiss = component::dismissSettings
                )
            }
        }
    }
}
