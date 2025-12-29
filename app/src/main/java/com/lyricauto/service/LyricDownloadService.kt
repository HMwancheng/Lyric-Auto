package com.lyricauto.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lyricauto.model.Lyric
import com.lyricauto.model.MusicInfo
import com.lyricauto.network.LyricDownloader
import com.lyricauto.utils.LyricCacheManager
import com.lyricauto.utils.SharedPreferencesManager
import kotlinx.coroutines.*

class LyricDownloadService : Service() {

    companion object {
        const val ACTION_LYRIC_DOWNLOADED = "com.lyricauto.ACTION_LYRIC_DOWNLOADED"
        const val EXTRA_LYRIC = "lyric"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cacheManager: LyricCacheManager
    private lateinit var prefsManager: SharedPreferencesManager
    private val lyricDownloader = LyricDownloader()

    override fun onCreate() {
        super.onCreate()
        cacheManager = LyricCacheManager(this)
        prefsManager = SharedPreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: ""
        val artist = intent?.getStringExtra("artist") ?: ""
        val album = intent?.getStringExtra("album") ?: ""

        if (title.isNotEmpty()) {
            downloadLyric(title, artist, album)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun downloadLyric(title: String, artist: String, album: String) {
        serviceScope.launch {
            try {
                val settings = prefsManager.getFloatWindowSettings()

                if (settings.enableCache) {
                    val cachedLyric = cacheManager.getLyric(title, artist)
                    if (cachedLyric != null) {
                        broadcastLyricDownloaded(cachedLyric, title, artist)
                        return@launch
                    }
                }

                val lyric = lyricDownloader.searchNeteaseLyric(title, artist)

                if (lyric != null && !lyric.isEmpty()) {
                    if (settings.enableCache) {
                        cacheManager.saveLyric(title, artist, lyric)
                    }
                    broadcastLyricDownloaded(lyric, title, artist)
                } else {
                    broadcastLyricDownloaded(Lyric(), title, artist)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                broadcastLyricDownloaded(Lyric(), title, artist)
            }
        }
    }

    private fun broadcastLyricDownloaded(lyric: Lyric, title: String, artist: String) {
        val intent = Intent(ACTION_LYRIC_DOWNLOADED).apply {
            putExtra(EXTRA_LYRIC, lyric)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ARTIST, artist)
        }
        sendBroadcast(intent)
    }
}
