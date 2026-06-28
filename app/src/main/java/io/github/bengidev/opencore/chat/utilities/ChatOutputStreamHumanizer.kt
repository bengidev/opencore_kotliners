package io.github.bengidev.opencore.chat.utilities

/** Translates raw CLI commands into compact timeline labels. */
internal object ChatOutputStreamHumanizer {
    data class Info(
        val verb: String,
        val target: String,
    )

    fun humanize(raw: String, isRunning: Boolean): Info {
        val command = unwrapShell(raw)
        val (tool, args) = splitToolAndArgs(command)

        return when (tool) {
            "cat", "nl", "head", "tail", "sed", "less", "more" -> Info(
                verb = if (isRunning) "Reading" else "Read",
                target = lastPathComponents(args, "file"),
            )
            "rg", "grep", "ag", "ack" -> Info(
                verb = if (isRunning) "Searching" else "Searched",
                target = searchSummary(args),
            )
            "ls" -> Info(
                verb = if (isRunning) "Listing" else "Listed",
                target = lastPathComponents(args, "directory"),
            )
            "find", "fd" -> Info(
                verb = if (isRunning) "Finding" else "Found",
                target = lastPathComponents(args, "files"),
            )
            "mkdir" -> Info(
                verb = if (isRunning) "Creating" else "Created",
                target = lastPathComponents(args, "directory"),
            )
            "rm" -> Info(
                verb = if (isRunning) "Removing" else "Removed",
                target = lastPathComponents(args, "file"),
            )
            "cp" -> Info(
                verb = if (isRunning) "Copying" else "Copied",
                target = lastPathComponents(args, "file"),
            )
            "mv" -> Info(
                verb = if (isRunning) "Moving" else "Moved",
                target = lastPathComponents(args, "file"),
            )
            "git" -> gitInfo(args, isRunning)
            else -> Info(
                verb = if (isRunning) "Running" else "Ran",
                target = command,
            )
        }
    }

    private fun unwrapShell(raw: String): String {
        var result = raw.trim()
        val lowered = result.lowercase()
        val shellPrefixes = listOf(
            "/usr/bin/bash -lc ",
            "/usr/bin/bash -c ",
            "/bin/bash -lc ",
            "/bin/bash -c ",
            "bash -lc ",
            "bash -c ",
            "/bin/sh -c ",
            "sh -c ",
        )

        for (prefix in shellPrefixes) {
            if (!lowered.startsWith(prefix)) continue
            result = result.drop(prefix.length)
            if ((result.startsWith("\"") && result.endsWith("\"")) ||
                (result.startsWith("'") && result.endsWith("'"))
            ) {
                result = result.drop(1).dropLast(1)
            }
            val andIndex = result.indexOf("&&")
            if (andIndex >= 0) {
                result = result.substring(andIndex + 2).trim()
            }
            break
        }

        val pipeIndex = result.indexOf(" | ")
        if (pipeIndex >= 0) {
            result = result.substring(0, pipeIndex).trim()
        }

        return result
    }

    private fun splitToolAndArgs(command: String): Pair<String, String> {
        val parts = command.split(" ", limit = 2)
        val rawTool = parts.firstOrNull().orEmpty().ifEmpty { command }
        val tool = rawTool.substringAfterLast('/').lowercase()
        val args = parts.getOrNull(1).orEmpty()
        return tool to args
    }

    private fun lastPathComponents(args: String, fallback: String): String {
        val tokens = args.split(" ")
        for (token in tokens.asReversed()) {
            val value = token.trim('"', '\'')
            if (value.isEmpty() || value.startsWith("-")) continue
            return compactPath(value)
        }
        return fallback
    }

    private fun compactPath(path: String): String {
        val components = path.split('/').filter { it.isNotEmpty() }
        if (components.size <= 2) return path
        return components.takeLast(2).joinToString("/")
    }

    private fun searchSummary(args: String): String {
        val tokens = args.split(" ")
        var pattern: String? = null
        var path: String? = null

        for (token in tokens) {
            val value = token.trim('"', '\'')
            if (value.isEmpty() || value.startsWith("-")) continue
            if (pattern == null) {
                pattern = if (value.length > 30) value.take(27) + "..." else value
            } else if (path == null) {
                path = compactPath(value)
            }
        }

        return when {
            pattern != null && path != null -> "for $pattern in $path"
            pattern != null -> "for $pattern"
            else -> "..."
        }
    }

    private fun gitInfo(args: String, isRunning: Boolean): Info {
        val parts = args.split(" ", limit = 2)
        val sub = parts.firstOrNull().orEmpty()

        return when (sub) {
            "status" -> Info(
                verb = if (isRunning) "Checking" else "Checked",
                target = "git status",
            )
            "diff" -> Info(
                verb = if (isRunning) "Comparing" else "Compared",
                target = "changes",
            )
            "log" -> Info(
                verb = if (isRunning) "Viewing" else "Viewed",
                target = "git log",
            )
            "add" -> Info(
                verb = if (isRunning) "Staging" else "Staged",
                target = "changes",
            )
            "commit" -> Info(
                verb = if (isRunning) "Committing" else "Committed",
                target = "changes",
            )
            "push" -> Info(
                verb = if (isRunning) "Pushing" else "Pushed",
                target = "to remote",
            )
            "pull" -> Info(
                verb = if (isRunning) "Pulling" else "Pulled",
                target = "from remote",
            )
            "checkout", "switch" -> {
                val branch = parts.getOrNull(1)
                    ?.split(" ")
                    ?.lastOrNull()
                    .orEmpty()
                Info(
                    verb = if (isRunning) "Switching to" else "Switched to",
                    target = branch.ifEmpty { "branch" },
                )
            }
            else -> Info(
                verb = if (isRunning) "Running" else "Ran",
                target = "git $args",
            )
        }
    }
}
