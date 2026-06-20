package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SessionItem
import io.github.bengidev.opencore.sidepanel.domain.SessionRepository
import io.github.bengidev.opencore.sidepanel.domain.SessionSamples

internal class InMemorySessionRepository(
    private var sessions: List<SessionItem> = SessionSamples.sessions
) : SessionRepository {
    override suspend fun loadSessions(): List<SessionItem> = sessions
    override suspend fun renameSession(id: String, newTitle: String) {
        sessions = sessions.map { if (it.id == id) it.copy(title = newTitle) else it }
    }
    override suspend fun deleteSession(id: String) {
        sessions = sessions.filterNot { it.id == id }
    }
}
