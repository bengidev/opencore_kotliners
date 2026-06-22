package io.github.bengidev.opencore.sidepanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SidePanelSessionSectionTest {

    private val now = Instant.ofEpochSecond(1_700_000_000)

    private fun conversation(
        title: String,
        updatedDaysAgo: Double,
        isPinned: Boolean = false,
        groupName: String? = null
    ): SidePanelConversation {
        val updatedAt = now.minusSeconds((updatedDaysAgo * 86_400).toLong())
        return SidePanelConversation(
            id = UUID.randomUUID(),
            title = title,
            createdAt = updatedAt.minusSeconds(10),
            updatedAt = updatedAt,
            isPinned = isPinned,
            groupName = groupName
        )
    }

    @Test
    fun pinnedSectionLeadsAndExcludesFromBuckets() {
        val conversations = listOf(
            conversation(title = "Pinned chat", updatedDaysAgo = 3.0, isPinned = true),
            conversation(title = "Today chat", updatedDaysAgo = 0.0)
        )
        val sections = SidePanelSessionSection.grouped(conversations, now = now)

        assertEquals("Pinned", sections.first().title)
        assertEquals(1, sections.first().conversations.size)
        val nonPinned = sections.drop(1).flatMap { it.conversations }
        assertTrue(nonPinned.all { !it.isPinned })
    }

    @Test
    fun recencyBucketsInCanonicalOrder() {
        val conversations = listOf(
            conversation(title = "older", updatedDaysAgo = 60.0),
            conversation(title = "today", updatedDaysAgo = 0.0),
            conversation(title = "last week", updatedDaysAgo = 5.0),
            conversation(title = "yesterday", updatedDaysAgo = 1.0),
            conversation(title = "last month", updatedDaysAgo = 20.0)
        )
        val titles = SidePanelSessionSection.grouped(conversations, now = now).map { it.title }
        assertEquals(
            listOf("Today", "Yesterday", "Previous 7 Days", "Previous 30 Days", "Older"),
            titles
        )
    }

    @Test
    fun emptyBucketsDropped() {
        val conversations = listOf(conversation(title = "today", updatedDaysAgo = 0.0))
        val titles = SidePanelSessionSection.grouped(conversations, now = now).map { it.title }
        assertEquals(listOf("Today"), titles)
    }

    @Test
    fun pinnedGroupedAppearsOnlyInPinned() {
        val conversations = listOf(
            conversation(title = "Pinned work", updatedDaysAgo = 0.0, isPinned = true, groupName = "Work"),
            conversation(title = "Today chat", updatedDaysAgo = 0.0)
        )
        val sections = SidePanelSessionSection.grouped(
            conversations,
            now = now,
            expandedGroups = setOf("Work")
        )
        val titles = sections.map { it.title }
        assertEquals(listOf("Pinned", "Today"), titles)
        assertEquals(listOf("Pinned work"), sections.first().conversations.map { it.title })
    }

    @Test
    fun pinnedExcludedFromGroupAndRecency() {
        val conversations = listOf(
            conversation(title = "Pinned", updatedDaysAgo = 0.0, isPinned = true, groupName = "Work"),
            conversation(title = "Work chat", updatedDaysAgo = 0.0, groupName = "Work"),
            conversation(title = "Today chat", updatedDaysAgo = 0.0)
        )
        val sections = SidePanelSessionSection.grouped(
            conversations,
            now = now,
            expandedGroups = setOf("Work")
        )
        val allTitles = sections.flatMap { it.conversations }.map { it.title }
        assertEquals(setOf("Pinned", "Work chat", "Today chat"), allTitles.toSet())
        assertEquals(1, allTitles.count { it == "Pinned" })
    }

    @Test
    fun forceExpandedGroupsExposeMatchingConversations() {
        val conversations = listOf(
            conversation(title = "Needle", updatedDaysAgo = 0.0, groupName = "Work")
        )

        val collapsed = SidePanelSessionSection.grouped(conversations, now = now)
        assertEquals(">:Work", collapsed.first().title)
        assertTrue(collapsed.first().conversations.isEmpty())

        val expandedForSearch = SidePanelSessionSection.grouped(
            conversations,
            now = now,
            forceExpandGroups = true
        )
        assertEquals("v:Work", expandedForSearch.first().title)
        assertEquals(listOf("Needle"), expandedForSearch.first().conversations.map { it.title })
    }

    @Test
    fun relativeLabelCompact() {
        assertEquals("now", SidePanelSessionSection.relativeLabel(now, now))
        assertEquals("1m", SidePanelSessionSection.relativeLabel(now.minusSeconds(90), now))
        assertEquals("3h", SidePanelSessionSection.relativeLabel(now.minusSeconds(3 * 3600), now))
        assertEquals("2d", SidePanelSessionSection.relativeLabel(now.minusSeconds(2 * 86_400), now))
        assertEquals("2w", SidePanelSessionSection.relativeLabel(now.minusSeconds(14 * 86_400), now))
        assertEquals("1y", SidePanelSessionSection.relativeLabel(now.minusSeconds(400 * 86_400), now))
    }
}
