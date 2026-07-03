package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SidePanelSettingSheet(
    component: SidePanelSettingComponent,
    onDismiss: () -> Unit,
) {
    val state by component.state.subscribeAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { component.onAppear() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeTheme.palette.surfaceBase,
    ) {
        SidePanelSettingContent(
            state = state,
            onDismiss = onDismiss,
            onProviderSelected = component::selectProvider,
            onDraftChanged = component::onDraftChanged,
            onSave = component::save,
            onClear = component::clear,
        )
    }
}
