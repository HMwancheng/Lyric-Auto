package com.lyricauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lyricauto.databinding.ActivityMainBinding
import com.lyricauto.service.LyricDownloadService
import com.lyricauto.service.LyricFloatService
import com.lyricauto.service.MusicListenerService
import com.lyricauto.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isFloatWindowEnabled = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (PermissionHelper.hasOverlayPermission(this)) {
            startFloatWindow()
        } else {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }

    private val lyricDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LyricDownloadService.ACTION_LYRIC_DOWNLOADED -> {
                    val lyric = intent.getParcelableExtra<com.lyricauto.model.Lyric>(LyricDownloadService.EXTRA_LYRIC)
                    val title = intent.getStringExtra(LyricDownloadService.EXTRA_TITLE) ?: ""
                    val artist = intent.getStringExtra(LyricDownloadService.EXTRA_ARTIST) ?: ""

                    if (lyric != null && !lyric.isEmpty()) {
                        Toast.makeText(this@MainActivity, "歌词下载成功: $title - $artist", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "未找到歌词: $title - $artist", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val musicChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicListenerService.ACTION_MUSIC_CHANGED -> {
                    val musicInfo = intent.getParcelableExtra<com.lyricauto.model.MusicInfo>(MusicListenerService.EXTRA_MUSIC_INFO)
                    musicInfo?.let {
                        updateCurrentMusicDisplay(it)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupClickListeners()
            registerReceivers()
            checkFloatWindowStatus()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkFloatWindowStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun setupClickListeners() {
        binding.toggleFloatWindowBtn.setOnClickListener {
            toggleFloatWindow()
        }

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.searchLyricsBtn.setOnClickListener {
            startActivity(Intent(this, LyricSearchActivity::class.java))
        }

        binding.localMusicBtn.setOnClickListener {
            startActivity(Intent(this, LocalMusicActivity::class.java))
        }
    }

    private fun toggleFloatWindow() {
        if (isFloatWindowEnabled) {
            stopFloatWindow()
        } else {
            if (PermissionHelper.hasOverlayPermission(this)) {
                startFloatWindow()
            } else {
                PermissionHelper.requestOverlayPermission(this, overlayPermissionLauncher)
            }
        }
    }

    private fun startFloatWindow() {
        startService(Intent(this, MusicListenerService::class.java))
        startService(Intent(this, LyricFloatService::class.java))
        isFloatWindowEnabled = true
        updateFloatWindowButton()
    }

    private fun stopFloatWindow() {
        stopService(Intent(this, LyricFloatService::class.java))
        stopService(Intent(this, MusicListenerService::class.java))
        isFloatWindowEnabled = false
        updateFloatWindowButton()
    }

    private fun checkFloatWindowStatus() {
        isFloatWindowEnabled = isServiceRunning(LyricFloatService::class.java)
        updateFloatWindowButton()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateFloatWindowButton() {
        binding.toggleFloatWindowBtn.text = if (isFloatWindowEnabled) {
            getString(R.string.float_window_enabled)
        } else {
            getString(R.string.enable_float_window)
        }
    }

    private fun updateCurrentMusicDisplay(musicInfo: com.lyricauto.model.MusicInfo) {
        binding.currentMusicText.text = getString(
            R.string.music_playing,
            musicInfo.title,
            musicInfo.artist
        )
    }

    private fun registerReceivers() {
        val lyricFilter = IntentFilter(LyricDownloadService.ACTION_LYRIC_DOWNLOADED)
        registerReceiver(lyricDownloadReceiver, lyricFilter)

        val musicFilter = IntentFilter(MusicListenerService.ACTION_MUSIC_CHANGED)
        registerReceiver(musicChangedReceiver, musicFilter)
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(lyricDownloadReceiver)
            unregisterReceiver(musicChangedReceiver)
        } catch (e: Exception) {
        }
    }
}
