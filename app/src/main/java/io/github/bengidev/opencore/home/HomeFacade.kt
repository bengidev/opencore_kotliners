package io.github.bengidev.opencore.home

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.home.application.HomeComponent

internal class HomeFacade {
    fun createComponent(
        componentContext: ComponentContext,
        onSendMessage: ((String) -> Unit)? = null
    ): HomeComponent = HomeComponent(
        componentContext = componentContext,
        onSendMessage = onSendMessage
    )
}
