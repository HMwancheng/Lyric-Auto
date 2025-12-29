package com.lyricauto.utils

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.lyricauto.NotificationListenerService
import com.lyricauto.model.MusicInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MusicMetadataExtractor(private val context: Context) {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessions: List<MediaController>? = null

    init {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        } catch (e: Exception) {
        }
    }

    fun release() {
        activeSessions = null
        mediaSessionManager = null
    }

    suspend fun getCurrentPlayingMusic(): MusicInfo? = suspendCancellableCoroutine { continuation ->
        try {
            val sessions = mediaSessionManager?.getActiveSessions(
                android.content.ComponentName(context, NotificationListenerService::class.java)
            )

            sessions?.forEach { controller ->
                val playbackState = controller.playbackState
                val metadata = controller.metadata

                if (playbackState?.state == PlaybackState.STATE_PLAYING && metadata != null) {
                    val musicInfo = extractMusicInfo(metadata, playbackState)
                    if (musicInfo.isValid()) {
                        continuation.resume(musicInfo)
                        return@suspendCancellableCoroutine
                    }
                }
            }

            continuation.resume(null)
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    private fun extractMusicInfo(metadata: MediaMetadata, playbackState: PlaybackState): MusicInfo {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val position = playbackState.position

        return MusicInfo(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            isPlaying = playbackState.state == PlaybackState.STATE_PLAYING,
            position = position
        )
    }
}
