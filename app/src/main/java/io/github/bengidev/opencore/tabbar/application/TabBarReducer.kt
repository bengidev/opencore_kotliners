package io.github.bengidev.opencore.tabbar.application

internal object TabBarReducer {
    fun reduce(state: TabBarState, intent: TabBarIntent): TabBarState = when (intent) {
        is TabBarIntent.TabSelected -> state.copy(selectedTab = intent.tab)
    }
}
