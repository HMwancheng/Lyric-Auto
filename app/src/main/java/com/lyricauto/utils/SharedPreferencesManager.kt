package com.lyricauto.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.lyricauto.model.FloatWindowSettings

class SharedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "lyric_auto_prefs",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    fun saveFloatWindowSettings(settings: FloatWindowSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString("float_window_settings", json).apply()
    }

    fun getFloatWindowSettings(): FloatWindowSettings {
        val json = prefs.getString("float_window_settings", null)
        return if (json != null) {
            gson.fromJson(json, FloatWindowSettings::class.java)
        } else {
            FloatWindowSettings()
        }
    }

    fun saveCurrentMusic(title: String, artist: String) {
        prefs.edit()
            .putString("current_title", title)
            .putString("current_artist", artist)
            .apply()
    }

    fun getCurrentMusic(): Pair<String, String> {
        val title = prefs.getString("current_title", "") ?: ""
        val artist = prefs.getString("current_artist", "") ?: ""
        return Pair(title, artist)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
