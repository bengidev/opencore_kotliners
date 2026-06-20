package io.github.bengidev.opencore.home

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.home.presenter.HomeView
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme
import io.github.bengidev.opencore.sidepanel.SidePanelDrawer
import io.github.bengidev.opencore.sidepanel.SidePanelSettingsSheetRoute
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    component: HomeComponent,
    sidePanelComponent: SidePanelComponent,
    darkTheme: Boolean
) {
    val state by component.state.subscribeAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidePanelDrawer(
                component = sidePanelComponent,
                darkTheme = darkTheme
            )
        }
    ) {
        OpenCoreHomeTheme(darkTheme = darkTheme) {
            HomeView(
                state = state,
                onDraftMessageChanged = component::onDraftMessageChanged,
                onSidebarTapped = { scope.launch { drawerState.open() } },
                onNewConversationTapped = component::onNewConversationTapped,
                onAttachmentTapped = component::onAttachmentTapped,
                onMicrophoneTapped = component::onMicrophoneTapped,
                onSendTapped = component::onSendTapped,
                onModelSelectorTapped = component::onModelSelectorTapped,
                onSpeedModeTapped = component::onSpeedModeTapped,
                onContextUsageTapped = component::onContextUsageTapped
            )
            SidePanelSettingsSheetRoute(
                component = sidePanelComponent,
                darkTheme = darkTheme
            )
        }
    }
}
