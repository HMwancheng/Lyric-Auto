package com.lyricauto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.lyricauto.R
import com.lyricauto.model.FloatWindowSettings
import com.lyricauto.model.Lyric
import com.lyricauto.model.LyricLine
import com.lyricauto.model.MusicInfo
import com.lyricauto.utils.SharedPreferencesManager
import kotlinx.coroutines.*

class LyricFloatService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "lyric_float_channel"
        const val ACTION_UPDATE_LYRIC = "com.lyricauto.ACTION_UPDATE_LYRIC"
        const val ACTION_UPDATE_SETTINGS = "com.lyricauto.ACTION_UPDATE_SETTINGS"
        const val EXTRA_LYRIC = "lyric"
        const val EXTRA_CURRENT_TIME = "current_time"
        const val EXTRA_MUSIC_INFO = "music_info"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private var lyricText: TextView? = null
    private var controlPanel: LinearLayout? = null
    private var currentLyric: Lyric? = null
    private var currentMusicInfo: MusicInfo? = null
    private var currentTime: Long = 0
    private var settings: FloatWindowSettings = FloatWindowSettings()
    private val prefsManager: SharedPreferencesManager by lazy { SharedPreferencesManager(this) }

    private val musicUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicListenerService.ACTION_MUSIC_CHANGED -> {
                    val musicInfo = intent.getParcelableExtra<MusicInfo>(MusicListenerService.EXTRA_MUSIC_INFO)
                    musicInfo?.let {
                        currentMusicInfo = it
                        prefsManager.saveCurrentMusic(it.title, it.artist)
                        requestLyric(it)
                    }
                }
                ACTION_UPDATE_LYRIC -> {
                    val lyric = intent.getParcelableExtra<Lyric>(EXTRA_LYRIC)
                    val time = intent.getLongExtra(EXTRA_CURRENT_TIME, 0)
                    val musicInfo = intent.getParcelableExtra<MusicInfo>(EXTRA_MUSIC_INFO)
                    lyric?.let {
                        currentLyric = it
                    }
                    musicInfo?.let {
                        currentMusicInfo = it
                    }
                    currentTime = time
                    updateLyricDisplay()
                }
                ACTION_UPDATE_SETTINGS -> {
                    settings = prefsManager.getFloatWindowSettings()
                    applySettings()
                }
            }
        }
    }

    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settings = prefsManager.getFloatWindowSettings()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatWindow()
        registerReceivers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        lyricUpdateJob?.cancel()
        serviceScope.cancel()
        removeFloatWindow()
        unregisterReceivers()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮歌词服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示悬浮歌词"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮歌词")
            .setContentText("正在显示歌词")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createFloatWindow() {
        val layoutInflater = LayoutInflater.from(this)
        floatView = layoutInflater.inflate(R.layout.float_lyric, null)

        lyricText = floatView?.findViewById(R.id.lyricText)
        controlPanel = floatView?.findViewById(R.id.controlPanel)

        setupControlButtons()
        setupDragListener()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        updateLayoutParams(params)
        applySettings()

        windowManager.addView(floatView, params)
    }

    private fun updateLayoutParams(params: WindowManager.LayoutParams) {
        when (settings.positionType) {
            FloatWindowSettings.PositionType.TOP -> {
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.y = 100
            }
            FloatWindowSettings.PositionType.CENTER -> {
                params.gravity = Gravity.CENTER
            }
            FloatWindowSettings.PositionType.BOTTOM -> {
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.y = 200
            }
            FloatWindowSettings.PositionType.CUSTOM -> {
                params.gravity = Gravity.TOP or Gravity.START
                params.x = settings.customX
                params.y = settings.customY
            }
        }
    }

    private fun setupControlButtons() {
        floatView?.findViewById<ImageButton>(R.id.moveUpBtn)?.setOnClickListener {
            moveWindow(0, -10)
        }
        floatView?.findViewById<ImageButton>(R.id.moveDownBtn)?.setOnClickListener {
            moveWindow(0, 10)
        }
        floatView?.findViewById<ImageButton>(R.id.moveLeftBtn)?.setOnClickListener {
            moveWindow(-10, 0)
        }
        floatView?.findViewById<ImageButton>(R.id.moveRightBtn)?.setOnClickListener {
            moveWindow(10, 0)
        }

        floatView?.setOnClickListener {
            controlPanel?.visibility = if (controlPanel?.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun setupDragListener() {
        floatView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val params = view.layoutParams as WindowManager.LayoutParams
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()
                        windowManager.updateViewLayout(view, params)

                        settings.customX = params.x
                        settings.customY = params.y
                        settings.positionType = FloatWindowSettings.PositionType.CUSTOM
                        prefsManager.saveFloatWindowSettings(settings)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun moveWindow(dx: Int, dy: Int) {
        floatView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x += dx
            params.y += dy
            windowManager.updateViewLayout(view, params)

            settings.customX = params.x
            settings.customY = params.y
            settings.positionType = FloatWindowSettings.PositionType.CUSTOM
            prefsManager.saveFloatWindowSettings(settings)
        }
    }

    private fun applySettings() {
        lyricText?.textSize = settings.fontSize.toFloat()
        lyricText?.setTextColor(settings.textColor)

        val container = floatView?.findViewById<FrameLayout>(R.id.floatContainer)
        container?.setBackgroundColor(settings.backgroundColor)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(MusicListenerService.ACTION_MUSIC_CHANGED)
            addAction(ACTION_UPDATE_LYRIC)
            addAction(ACTION_UPDATE_SETTINGS)
        }
        registerReceiver(musicUpdateReceiver, filter)
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(musicUpdateReceiver)
        } catch (e: Exception) {
        }
    }

    private fun removeFloatWindow() {
        try {
            floatView?.let {
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
        }
    }

    private fun requestLyric(musicInfo: MusicInfo) {
        val intent = Intent(this, LyricDownloadService::class.java).apply {
            putExtra("title", musicInfo.title)
            putExtra("artist", musicInfo.artist)
            putExtra("album", musicInfo.album)
        }
        startService(intent)
    }

    private fun updateLyricDisplay() {
        serviceScope.launch {
            while (isActive) {
                currentLyric?.let { lyric ->
                    currentMusicInfo?.let { musicInfo ->
                        if (musicInfo.isPlaying) {
                            currentTime = musicInfo.position
                        }
                        val currentLine = lyric.getCurrentLine(currentTime)
                        currentLine?.let { line ->
                            updateLyricText(line.text, lyric.getLineAtTime(currentTime))
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun updateLyricText(text: String, lineIndex: Int) {
        lyricText?.text = text
        lyricText?.setTextColor(if (lineIndex >= 0) settings.currentLineColor else settings.textColor)

        when (settings.animationType) {
            FloatWindowSettings.AnimationType.FADE -> {
                lyricText?.alpha = 0f
                lyricText?.animate()?.alpha(1f)?.duration = 300
            }
            FloatWindowSettings.AnimationType.SLIDE -> {
                lyricText?.translationY = 20f
                lyricText?.animate()?.translationY(0f)?.duration = 300
            }
            FloatWindowSettings.AnimationType.SCALE -> {
                lyricText?.scaleX = 0.8f
                lyricText?.scaleY = 0.8f
                lyricText?.animate()?.scaleX(1f)?.scaleY(1f)?.duration = 300
            }
            FloatWindowSettings.AnimationType.NONE -> {
            }
        }
    }
}
