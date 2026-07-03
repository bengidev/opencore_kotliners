package io.github.bengidev.opencore.tabbar

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.tabbar.application.TabBarComponent

internal class TabBarFacade {
    fun createComponent(componentContext: ComponentContext): TabBarComponent =
        TabBarComponent(componentContext = componentContext)
}
