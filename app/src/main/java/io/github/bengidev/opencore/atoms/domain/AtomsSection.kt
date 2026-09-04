package io.github.bengidev.opencore.atoms.domain

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class AtomsSection(
    val id: String,
    val title: String,
    val entries: List<AtomListEntry>,
) {
    companion object {
        fun grouped(
            entries: List<AtomListEntry>,
            now: Instant = Instant.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            expandedGroups: Set<String> = emptySet(),
            forceExpandGroups: Boolean = false,
        ): List<AtomsSection> {
            val sections = mutableListOf<AtomsSection>()

            val pinned = entries.filter { it.atom.isPinned }
            if (pinned.isNotEmpty()) {
                sections += AtomsSection(id = "pinned", title = "Pinned", entries = pinned)
            }

            val groupBuckets = linkedMapOf<String, MutableList<AtomListEntry>>()
            for (entry in entries) {
                if (!entry.atom.isPinned && entry.atom.groupName != null) {
                    groupBuckets.getOrPut(entry.atom.groupName) { mutableListOf() }.add(entry)
                }
            }
            for (groupName in groupBuckets.keys.sorted()) {
                val groupEntries = groupBuckets[groupName].orEmpty()
                val isExpanded = forceExpandGroups || expandedGroups.contains(groupName)
                val prefix = if (isExpanded) "v:" else ">:"
                sections += AtomsSection(
                    id = "group:$groupName",
                    title = prefix + groupName,
                    entries = if (isExpanded) groupEntries else emptyList(),
                )
            }

            val buckets = linkedMapOf<RecencyBucket, MutableList<AtomListEntry>>()
            for (entry in entries) {
                if (!entry.atom.isPinned && entry.atom.groupName == null) {
                    val bucket = RecencyBucket.classify(entry.lastMessageAt, now, zoneId)
                    buckets.getOrPut(bucket) { mutableListOf() }.add(entry)
                }
            }
            for (bucket in RecencyBucket.entries) {
                val bucketEntries = buckets[bucket] ?: continue
                sections += AtomsSection(
                    id = bucket.id,
                    title = bucket.title,
                    entries = bucketEntries,
                )
            }

            return sections
        }

        fun relativeLabel(date: Instant, now: Instant = Instant.now()): String {
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
            val today = now.atZone(zoneId).toLocalDate()
            val dateDay = date.atZone(zoneId).toLocalDate()
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

internal fun AtomListEntry.toConversation(): SidePanelConversation = atom.toSidePanelConversation()
