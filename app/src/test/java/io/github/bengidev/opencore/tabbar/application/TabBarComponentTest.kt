package io.github.bengidev.opencore.tabbar.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.tabbar.domain.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Test

class TabBarComponentTest {

    @Test
    fun selectTab_changesCurrentTab() {
        val component = TabBarComponent(DefaultComponentContext(LifecycleRegistry()))

        component.selectTab(HomeTab.ABOUT)

        assertEquals(HomeTab.ABOUT, component.state.value.selectedTab)
    }
}
