package io.github.bengidev.opencore.sidepanel.domain

internal object SessionSamples {
    val sessions: List<SessionItem> = listOf(
        SessionItem(id = "s1", title = "Welcome chat", preview = "How do I get started?", isActive = true),
        SessionItem(id = "s2", title = "Code review", preview = "Review this Kotlin reducer…", isActive = false),
        SessionItem(id = "s3", title = "Architecture notes", preview = "SidePanel module design", isActive = false)
    )
}
