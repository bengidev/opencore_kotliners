package io.github.bengidev.opencore.chat.presenter

import android.media.AudioAttributes
import android.media.MediaPlayer
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.chat.utilities.ChatVoiceNotePlaybackDisplayLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** Owns in-chat voice-note playback — play, pause, resume, and stop. */
internal class ChatVoiceNotePlaybackController(
    private val scope: CoroutineScope,
) {
    sealed class PlaybackState {
        data object Idle : PlaybackState()
        data class Playing(val attachmentId: UUID) : PlaybackState()
        data class Paused(val attachmentId: UUID) : PlaybackState()
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackCurrentTime = MutableStateFlow(0.0)
    val playbackCurrentTime: StateFlow<Double> = _playbackCurrentTime.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private var player: MediaPlayer? = null
    private var activeAttachmentId: UUID? = null
    private var activeDurationSeconds: Double = 0.0
    private var progressJob: Job? = null

    fun isPlaying(attachmentId: UUID): Boolean =
        _playbackState.value is PlaybackState.Playing &&
            (_playbackState.value as PlaybackState.Playing).attachmentId == attachmentId

    fun isActive(attachmentId: UUID): Boolean = when (val state = _playbackState.value) {
        is PlaybackState.Playing -> state.attachmentId == attachmentId
        is PlaybackState.Paused -> state.attachmentId == attachmentId
        PlaybackState.Idle -> false
    }

    fun displayedDuration(attachment: ChatMessageAttachment): Double =
        ChatVoiceNotePlaybackDisplayLogic.displayedDuration(
            currentTime = _playbackCurrentTime.value,
            totalDuration = resolvedDuration(attachment),
            isPlaybackActive = isActive(attachment.id),
        )

    fun playbackProgress(attachment: ChatMessageAttachment): Double {
        if (!isActive(attachment.id)) return 0.0
        return ChatVoiceNotePlaybackDisplayLogic.playbackProgress(
            currentTime = _playbackCurrentTime.value,
            duration = resolvedDuration(attachment),
        )
    }

    fun resolvedDurationSeconds(attachment: ChatMessageAttachment): Double =
        resolvedDuration(attachment)

    fun toggle(attachment: ChatMessageAttachment) {
        if (attachment.kind != ChatMessageAttachmentKind.AUDIO) return

        when (val state = _playbackState.value) {
            is PlaybackState.Playing if state.attachmentId == attachment.id -> pauseActivePlayback()
            is PlaybackState.Paused if state.attachmentId == attachment.id -> resumeActivePlayback()
            else -> {
                stop()
                startPlayback(attachment)
            }
        }
    }

    fun stop() {
        stopProgressUpdates()
        player?.stop()
        player?.release()
        player = null
        activeAttachmentId = null
        activeDurationSeconds = 0.0
        _playbackCurrentTime.value = 0.0
        _playbackState.value = PlaybackState.Idle
    }

    fun release() {
        stop()
    }

    private fun startPlayback(attachment: ChatMessageAttachment) {
        _lastErrorMessage.value = null
        val audioFile = File(attachment.localPath)
        if (!audioFile.exists() || audioFile.length() <= 0L) {
            _lastErrorMessage.value = "Voice note is no longer available."
            return
        }

        val mediaPlayer = MediaPlayer()
        runCatching {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mediaPlayer.setDataSource(attachment.localPath)
            mediaPlayer.prepare()
            val resolvedDuration = resolvedDuration(
                attachment = attachment,
                playerDurationSeconds = mediaPlayer.duration / 1000.0,
            )
            if (resolvedDuration <= 0.0) {
                mediaPlayer.release()
                _lastErrorMessage.value = "Voice note could not be played."
                return
            }
            activeDurationSeconds = resolvedDuration
            activeAttachmentId = attachment.id
            mediaPlayer.setOnCompletionListener { completedPlayer ->
                scope.launch { handlePlaybackCompleted(completedPlayer) }
            }
            mediaPlayer.setOnErrorListener { completedPlayer, _, _ ->
                scope.launch {
                    if (player === completedPlayer) {
                        _lastErrorMessage.value = "Voice note could not be played."
                        stop()
                    }
                }
                true
            }
            player = mediaPlayer
            _playbackCurrentTime.value = 0.0
            _playbackState.value = PlaybackState.Playing(attachment.id)
            mediaPlayer.start()
            syncProgressFromPlayer()
            startProgressUpdates()
        }.onFailure {
            mediaPlayer.release()
            _lastErrorMessage.value = "Voice note could not be played."
            stop()
        }
    }

    private fun pauseActivePlayback() {
        syncProgressFromPlayer()
        player?.pause()
        stopProgressUpdates()
        val current = _playbackState.value
        if (current is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Paused(current.attachmentId)
        }
    }

    private fun resumeActivePlayback() {
        player?.start()
        val current = _playbackState.value
        if (current is PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Playing(current.attachmentId)
        }
        syncProgressFromPlayer()
        startProgressUpdates()
    }

    private fun resolvedDuration(
        attachment: ChatMessageAttachment,
        playerDurationSeconds: Double? = null,
    ): Double {
        if (isActive(attachment.id) && activeDurationSeconds > 0) {
            return activeDurationSeconds
        }
        playerDurationSeconds?.takeIf { it > 0 }?.let { return it }
        return attachment.audioDurationSeconds.coerceAtLeast(0.0)
    }

    private fun syncProgressFromPlayer() {
        player?.let { activePlayer ->
            _playbackCurrentTime.value = activePlayer.currentPosition / 1000.0
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (true) {
                delay(50)
                if (_playbackState.value !is PlaybackState.Playing) return@launch
                val activePlayer = player ?: return@launch
                _playbackCurrentTime.value = activePlayer.currentPosition / 1000.0
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handlePlaybackCompleted(completedPlayer: MediaPlayer) {
        if (player !== completedPlayer) return
        stopProgressUpdates()
        runCatching { completedPlayer.seekTo(0) }
        completedPlayer.release()
        player = null
        activeDurationSeconds = 0.0
        _playbackCurrentTime.value = 0.0
        _playbackState.value = PlaybackState.Idle
    }
}
