package io.github.bengidev.opencore.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.home.presenter.HomeView
import io.github.bengidev.opencore.home.theme.OpenCoreHomeTheme

@Composable
internal fun HomeScreen(
    component: HomeComponent,
    darkTheme: Boolean
) {
    val state by component.state.subscribeAsState()

    OpenCoreHomeTheme(darkTheme = darkTheme) {
        HomeView(
            state = state,
            onDraftMessageChanged = component::onDraftMessageChanged,
            onSidebarTapped = component::onSidebarTapped,
            onNewConversationTapped = component::onNewConversationTapped,
            onAttachmentTapped = component::onAttachmentTapped,
            onMicrophoneTapped = component::onMicrophoneTapped,
            onSendTapped = component::onSendTapped,
            onModelSelectorTapped = component::onModelSelectorTapped,
            onSpeedModeTapped = component::onSpeedModeTapped,
            onContextUsageTapped = component::onContextUsageTapped
        )
    }
}
