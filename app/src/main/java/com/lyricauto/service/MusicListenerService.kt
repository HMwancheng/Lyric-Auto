package com.lyricauto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lyricauto.R
import com.lyricauto.model.MusicInfo
import com.lyricauto.utils.MusicMetadataExtractor
import kotlinx.coroutines.*

class MusicListenerService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "music_listener_channel"
        const val ACTION_MUSIC_CHANGED = "com.lyricauto.ACTION_MUSIC_CHANGED"
        const val EXTRA_MUSIC_INFO = "music_info"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var metadataExtractor: MusicMetadataExtractor? = null
    private var currentMusicInfo: MusicInfo? = null

    private val musicStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.android.music.metachanged",
                "com.android.music.playstatechanged",
                "com.android.music.playbackcomplete",
                "com.android.music.queuechanged",
                "com.htc.music.metachanged",
                "fm.last.android.metachanged",
                "com.sec.android.app.music.metachanged",
                "com.nullsoft.winamp.metachanged",
                "com.amazon.mp3.metachanged",
                "com.miui.player.metachanged",
                "com.real.IMP.metachanged",
                "com.sonyericsson.music.metachanged",
                "com.rdio.android.metachanged",
                "com.samsung.sec.android.MusicPlayer.metachanged",
                "com.andrew.apollo.metachanged",
                "in.krosbits.musicolet.metachanged",
                "net.protyposis.android.mediaplayer.metachanged" -> {
                    handleMusicMetadataChanged(intent)
                }
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                checkMusicState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        metadataExtractor = MusicMetadataExtractor(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerMusicStateReceiver()
        registerAudioFocusListener()
        checkMusicState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterMusicStateReceiver()
        metadataExtractor?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐监听服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监听系统音乐播放状态"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮歌词")
            .setContentText("正在监听音乐播放")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerMusicStateReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction("com.android.music.metachanged")
            addAction("com.android.music.playstatechanged")
            addAction("com.android.music.playbackcomplete")
            addAction("com.android.music.queuechanged")
            addAction("com.htc.music.metachanged")
            addAction("fm.last.android.metachanged")
            addAction("com.sec.android.app.music.metachanged")
            addAction("com.nullsoft.winamp.metachanged")
            addAction("com.amazon.mp3.metachanged")
            addAction("com.miui.player.metachanged")
            addAction("com.real.IMP.metachanged")
            addAction("com.sonyericsson.music.metachanged")
            addAction("com.rdio.android.metachanged")
            addAction("com.samsung.sec.android.MusicPlayer.metachanged")
            addAction("com.andrew.apollo.metachanged")
            addAction("in.krosbits.musicolet.metachanged")
            addAction("net.protyposis.android.mediaplayer.metachanged")
        }
        registerReceiver(musicStateReceiver, intentFilter)
    }

    private fun unregisterMusicStateReceiver() {
        try {
            unregisterReceiver(musicStateReceiver)
        } catch (e: Exception) {
        }
    }

    private fun registerAudioFocusListener() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    private fun handleMusicMetadataChanged(intent: Intent) {
        val title = intent.getStringExtra("track") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val album = intent.getStringExtra("album") ?: ""
        val isPlaying = intent.getBooleanExtra("playing", true)
        val duration = intent.getLongExtra("duration", 0)
        val position = intent.getLongExtra("position", 0)

        val musicInfo = MusicInfo(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            isPlaying = isPlaying,
            position = position
        )

        if (musicInfo.isValid() && musicInfo != currentMusicInfo) {
            currentMusicInfo = musicInfo
            broadcastMusicChanged(musicInfo)
        }
    }

    private fun checkMusicState() {
        serviceScope.launch(Dispatchers.IO) {
            val musicInfo = metadataExtractor?.getCurrentPlayingMusic()
            if (musicInfo?.isValid() == true && musicInfo != currentMusicInfo) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBroadcastTime > broadcastDebounceDelay) {
                    currentMusicInfo = musicInfo
                    lastBroadcastTime = currentTime
                    withContext(Dispatchers.Main) {
                        broadcastMusicChanged(musicInfo)
                    }
                }
            }
        }
    }

    private fun broadcastMusicChanged(musicInfo: MusicInfo) {
        val intent = Intent(ACTION_MUSIC_CHANGED).apply {
            putExtra(EXTRA_MUSIC_INFO, musicInfo)
        }
        sendBroadcast(intent)
    }
}
