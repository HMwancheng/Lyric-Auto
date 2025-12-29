package com.lyricauto.model

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FloatWindowSettings(
    val fontSize: Int = 16,
    val textColor: Int = Color.WHITE,
    val currentLineColor: Int = Color.parseColor("#FFD700"),
    val backgroundColor: Int = Color.parseColor("#80000000"),
    val animationType: AnimationType = AnimationType.FADE,
    val positionType: PositionType = PositionType.CENTER,
    val customX: Int = 0,
    val customY: Int = 0,
    val showInStatusBar: Boolean = false,
    val autoDownload: Boolean = true,
    val enableCache: Boolean = true
) : Parcelable {
    enum class AnimationType {
        NONE, FADE, SLIDE, SCALE
    }

    enum class PositionType {
        TOP, CENTER, BOTTOM, CUSTOM
    }
}
