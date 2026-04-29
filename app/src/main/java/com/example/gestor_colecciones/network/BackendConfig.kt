package com.example.gestor_colecciones.network

import android.content.Context
import android.os.Build

/**
 * Gestiona la URL base del backend usada por la app.
 *
 * En desarrollo mantiene un valor por defecto distinto para emulador y para
 * dispositivo real, pero permite sobrescribirlo y persistirlo para adaptarse a
 * cambios de red sin tocar codigo.
 */
object BackendConfig {

    private const val PREFS_NAME = "backend_prefs"
    private const val KEY_BASE_URL = "base_url"

    private const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8080/"
    private const val DEFAULT_DEVICE_BASE_URL = "http://192.168.0.110:8080/"

    @Volatile
    private var cachedBaseUrl: String? = null

    /** Devuelve la URL base actual, cargandola de preferencias si es necesario. */
    fun getBaseUrl(context: Context): String {
        val cached = cachedBaseUrl
        if (!cached.isNullOrBlank()) return cached

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_BASE_URL, null)
        val resolved = normalize(stored) ?: defaultBaseUrl()
        cachedBaseUrl = resolved
        return resolved
    }

    /** Devuelve la URL base actual ya resuelta en memoria. */
    fun currentBaseUrl(): String = cachedBaseUrl ?: defaultBaseUrl()

    /** Devuelve la URL base sin slash final, util para concatenar rutas relativas. */
    fun currentBaseUrlWithoutTrailingSlash(): String =
        currentBaseUrl().removeSuffix("/")

    /** Guarda una nueva URL base persistente para la API y la carga de imagenes. */
    fun saveBaseUrl(context: Context, rawUrl: String): String {
        val normalized = normalize(rawUrl)
            ?: throw IllegalArgumentException("URL del servidor no valida")

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()

        cachedBaseUrl = normalized
        return normalized
    }

    /** Restablece la URL por defecto segun el entorno de ejecucion. */
    fun resetToDefault(context: Context): String {
        val normalized = defaultBaseUrl()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BASE_URL)
            .apply()
        cachedBaseUrl = normalized
        return normalized
    }

    private fun normalize(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }

    private fun defaultBaseUrl(): String =
        if (isProbablyEmulator()) DEFAULT_EMULATOR_BASE_URL else DEFAULT_DEVICE_BASE_URL

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk"
    }
}
