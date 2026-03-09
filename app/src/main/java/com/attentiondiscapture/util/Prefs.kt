package com.attentiondiscapture.util

import android.content.Context

object Prefs {
    private const val NAME = "attention_discapture_prefs"
    private const val KEY_MAX_SCREENSHOTS = "max_screenshots"

    fun getMaxScreenshots(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MAX_SCREENSHOTS, 30)

    fun setMaxScreenshots(context: Context, value: Int) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MAX_SCREENSHOTS, value).apply()
}
