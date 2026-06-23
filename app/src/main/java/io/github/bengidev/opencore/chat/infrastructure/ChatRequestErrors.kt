package io.github.bengidev.opencore.chat.infrastructure

import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun formatChatRequestError(error: Exception): String {
    val root = error.rootCause()
    return when (root) {
        is UnknownHostException -> formatDnsLookupError(root)
        is SocketTimeoutException ->
            "Request timed out. Check your connection and try again."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: root.message?.takeIf { it.isNotBlank() }
            ?: "Request failed"
    }
}

private fun formatDnsLookupError(error: UnknownHostException): String {
    val host = error.unresolvedHost()
    return if (host != null) {
        "Couldn't reach $host (DNS lookup failed). Check your network and try again."
    } else {
        "Couldn't reach the server (DNS lookup failed). Check your network and try again."
    }
}

private fun UnknownHostException.unresolvedHost(): String? =
    message?.let { hostPattern.find(it)?.groupValues?.getOrNull(1) }

private fun Throwable.rootCause(): Throwable {
    var current = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private val hostPattern = Regex("""Unable to resolve host "([^"]+)"""")
