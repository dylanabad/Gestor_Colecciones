package com.example.gestor_colecciones.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.gestor_colecciones.R

enum class ThemePalette(
    val key: String,
    val displayName: String,
    val themeResId: Int
) {
    BLUE("blue", "Azul", R.style.Theme_Gestor_Colecciones),
    PURPLE("purple", "Morado", R.style.Theme_Gestor_Colecciones_Purple),
    GREEN("green", "Verde", R.style.Theme_Gestor_Colecciones_Green),
    ORANGE("orange", "Naranja", R.style.Theme_Gestor_Colecciones_Orange);

    companion object {
        fun fromKey(key: String?): ThemePalette {
            return entries.firstOrNull { it.key == key } ?: BLUE
        }
    }
}

enum class ThemeMode(
    val key: String,
    val displayName: String,
    val appCompatNightMode: Int
) {
    SYSTEM("system", "Sistema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", "Claro", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", "Oscuro", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }
}

object ThemePrefs {
    private const val PREFS = "app_prefs"
    private const val KEY_PALETTE = "theme_palette"
    private const val KEY_MODE = "theme_mode"

    fun getPalette(context: Context): ThemePalette {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ThemePalette.fromKey(prefs.getString(KEY_PALETTE, null))
    }

    fun setPalette(context: Context, palette: ThemePalette) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PALETTE, palette.key).apply()
    }

    fun getMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ThemeMode.fromKey(prefs.getString(KEY_MODE, null))
    }

    fun setMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.key).apply()
    }
}

