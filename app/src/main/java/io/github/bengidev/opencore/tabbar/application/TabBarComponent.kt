package io.github.bengidev.opencore.tabbar.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.bengidev.opencore.tabbar.domain.HomeTab

internal class TabBarComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext {

    private val _state = MutableValue(TabBarState())
    val state: Value<TabBarState> = _state

    fun selectTab(tab: HomeTab) = dispatch(TabBarIntent.TabSelected(tab))

    fun dispatch(intent: TabBarIntent) {
        _state.update { current -> TabBarReducer.reduce(current, intent) }
    }
}
