package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update

internal class HomeComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val _state = MutableValue(HomeState())
    val state: Value<HomeState> = _state

    fun dispatch(intent: HomeIntent) {
        _state.update { current -> HomeReducer.reduce(current, intent) }
    }

    fun onDraftMessageChanged(value: String) = dispatch(HomeIntent.DraftMessageChanged(value))
    fun onSidebarTapped() = dispatch(HomeIntent.SidebarTapped)
    fun onNewConversationTapped() = dispatch(HomeIntent.NewConversationTapped)
    fun onAttachmentTapped() = dispatch(HomeIntent.AttachmentTapped)
    fun onMicrophoneTapped() = dispatch(HomeIntent.MicrophoneTapped)
    fun onSendTapped() = dispatch(HomeIntent.SendTapped)
    fun onModelSelectorTapped() = dispatch(HomeIntent.ModelSelectorTapped)
    fun onSpeedModeTapped() = dispatch(HomeIntent.SpeedModeTapped)
    fun onContextUsageTapped() = dispatch(HomeIntent.ContextUsageTapped)
}
