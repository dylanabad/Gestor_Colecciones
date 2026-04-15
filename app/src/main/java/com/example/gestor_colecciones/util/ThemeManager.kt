package com.example.gestor_colecciones.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    
    // Almacena temporalmente una captura de la pantalla antes de recrear la activity
    private var pendingScreenshot: Bitmap? = null

    fun apply(activity: AppCompatActivity) {
        // Aplicar modo noche/claro
        val mode = ThemePrefs.getMode(activity)
        if (AppCompatDelegate.getDefaultNightMode() != mode.appCompatNightMode) {
            AppCompatDelegate.setDefaultNightMode(mode.appCompatNightMode)
        }

        // Aplicar paleta de colores
        val palette = ThemePrefs.getPalette(activity)
        activity.setTheme(palette.themeResId)
        
        // Si hay una captura pendiente, mostrarla y animar el desvanecimiento
        pendingScreenshot?.let { screenshot ->
            val rootView = activity.window.decorView as ViewGroup
            val imageView = ImageView(activity).apply {
                setImageBitmap(screenshot)
                scaleType = ImageView.ScaleType.FIT_XY
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            rootView.addView(imageView)
            
            // Animación fluida de salida
            imageView.animate()
                .alpha(0f)
                .setDuration(400) // Duración del cambio
                .withEndAction {
                    rootView.removeView(imageView)
                    pendingScreenshot = null
                }
                .start()
        }
    }

    /**
     * Llama a esto para cambiar la paleta con una animación fluida
     */
    fun updatePalette(activity: AppCompatActivity, newPalette: ThemePalette) {
        takeScreenshot(activity)
        ThemePrefs.setPalette(activity, newPalette)
        activity.recreate()
    }

    /**
     * Llama a esto para cambiar el modo (oscuro/claro) con animación
     */
    fun updateMode(activity: AppCompatActivity, newMode: ThemeMode) {
        takeScreenshot(activity)
        ThemePrefs.setMode(activity, newMode)
        // AppCompatDelegate.setDefaultNightMode recrea la activity automáticamente
        AppCompatDelegate.setDefaultNightMode(newMode.appCompatNightMode)
    }

    private fun takeScreenshot(activity: AppCompatActivity) {
        try {
            val view = activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            pendingScreenshot = bitmap
        } catch (e: Exception) {
            pendingScreenshot = null
        }
    }
}
