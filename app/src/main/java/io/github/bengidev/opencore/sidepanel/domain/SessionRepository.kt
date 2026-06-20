package io.github.bengidev.opencore.sidepanel.domain

internal interface SessionRepository {
    suspend fun loadSessions(): List<SessionItem>
    suspend fun renameSession(id: String, newTitle: String)
    suspend fun deleteSession(id: String)
}
