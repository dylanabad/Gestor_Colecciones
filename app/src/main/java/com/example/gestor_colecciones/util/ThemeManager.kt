package com.example.gestor_colecciones.util

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    fun apply(activity: AppCompatActivity) {
        val mode = ThemePrefs.getMode(activity)
        AppCompatDelegate.setDefaultNightMode(mode.appCompatNightMode)

        val palette = ThemePrefs.getPalette(activity)
        activity.setTheme(palette.themeResId)
    }
}

