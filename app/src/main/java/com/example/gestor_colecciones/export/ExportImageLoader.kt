package com.example.gestor_colecciones.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.gestor_colecciones.util.ImageUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resuelve imágenes para los generadores de exportación, soportando tanto
 * archivos locales como rutas remotas del backend.
 */
object ExportImageLoader {

    fun loadBitmap(path: String): Bitmap? {
        val model = ImageUtils.toGlideModel(path) ?: return null

        return when (model) {
            is File -> BitmapFactory.decodeFile(model.absolutePath)
            is String -> loadRemoteBitmap(model)
            else -> null
        }
    }

    private fun loadRemoteBitmap(url: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                doInput = true
                connect()
            }
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
