package com.parseus.codecinfo

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class InfoType(val tabPosition: Int,
                    @DrawableRes val tabIconResId: Int,
                    @StringRes val tabTextResId: Int
) {
    Audio(0, R.drawable.ic_audio, R.string.category_audio),
    Video(1, R.drawable.ic_video, R.string.category_video),
    DRM(2, R.drawable.ic_lock, R.string.category_drm);

    companion object {
        fun fromInt(value: Int): InfoType = when (value) {
            Audio.tabPosition -> Audio
            Video.tabPosition -> Video
            else -> DRM
        }

        val INFO_TYPE_COUNT: Int get() = if (Build.VERSION.SDK_INT >= 18) 3 else 2

        var currentInfoType: InfoType = Audio
    }
}