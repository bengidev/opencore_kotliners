package io.github.bengidev.opencore.sidepanel.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class SidePanelSessionSection(
    val id: String,
    val title: String,
    val conversations: List<SidePanelConversation>
) {
    companion object {
        fun grouped(
            conversations: List<SidePanelConversation>,
            now: Instant = Instant.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            expandedGroups: Set<String> = emptySet(),
            forceExpandGroups: Boolean = false
        ): List<SidePanelSessionSection> {
            val sections = mutableListOf<SidePanelSessionSection>()

            val pinned = conversations.filter { it.isPinned }
            if (pinned.isNotEmpty()) {
                sections += SidePanelSessionSection(id = "pinned", title = "Pinned", conversations = pinned)
            }

            val groupBuckets = linkedMapOf<String, MutableList<SidePanelConversation>>()
            for (conversation in conversations) {
                if (!conversation.isPinned && conversation.groupName != null) {
                    groupBuckets.getOrPut(conversation.groupName) { mutableListOf() }.add(conversation)
                }
            }
            for (groupName in groupBuckets.keys.sorted()) {
                val groupConversations = groupBuckets[groupName].orEmpty()
                val isExpanded = forceExpandGroups || expandedGroups.contains(groupName)
                val prefix = if (isExpanded) "v:" else ">:"
                sections += SidePanelSessionSection(
                    id = "group:$groupName",
                    title = prefix + groupName,
                    conversations = if (isExpanded) groupConversations else emptyList()
                )
            }

            val buckets = linkedMapOf<RecencyBucket, MutableList<SidePanelConversation>>()
            for (conversation in conversations) {
                if (!conversation.isPinned && conversation.groupName == null) {
                    val bucket = RecencyBucket.classify(conversation.updatedAt, now, zoneId)
                    buckets.getOrPut(bucket) { mutableListOf() }.add(conversation)
                }
            }
            for (bucket in RecencyBucket.entries) {
                val bucketConversations = buckets[bucket] ?: continue
                sections += SidePanelSessionSection(
                    id = bucket.id,
                    title = bucket.title,
                    conversations = bucketConversations
                )
            }

            return sections
        }

        fun relativeLabel(for date: Instant, now: Instant = Instant.now()): String {
            val intervalSeconds = (now.epochSecond - date.epochSecond).coerceAtLeast(0)
            val minute = 60L
            val hour = 60 * minute
            val day = 24 * hour
            val week = 7 * day
            val month = 30 * day
            val year = 365 * day

            return when {
                intervalSeconds >= year -> "${intervalSeconds / year}y"
                intervalSeconds >= month -> "${intervalSeconds / month}mo"
                intervalSeconds >= week -> "${intervalSeconds / week}w"
                intervalSeconds >= day -> "${intervalSeconds / day}d"
                intervalSeconds >= hour -> "${intervalSeconds / hour}h"
                intervalSeconds >= minute -> "${intervalSeconds / minute}m"
                else -> "now"
            }
        }
    }
}

private enum class RecencyBucket(val id: String, val title: String) {
    Today("today", "Today"),
    Yesterday("yesterday", "Yesterday"),
    Previous7Days("previous7Days", "Previous 7 Days"),
    Previous30Days("previous30Days", "Previous 30 Days"),
    Older("older", "Older");

    companion object {
        fun classify(date: Instant, now: Instant, zoneId: ZoneId): RecencyBucket {
            val today = LocalDate.ofInstant(now, zoneId)
            val dateDay = LocalDate.ofInstant(date, zoneId)
            val daysAgo = ChronoUnit.DAYS.between(dateDay, today).toInt()
            return when {
                daysAgo <= 0 -> Today
                daysAgo == 1 -> Yesterday
                daysAgo <= 7 -> Previous7Days
                daysAgo <= 30 -> Previous30Days
                else -> Older
            }
        }
    }
}
