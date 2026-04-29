package com.example.gestor_colecciones.util

import com.example.gestor_colecciones.network.BackendConfig
import java.io.File

/** Utilidad para normalizar rutas de imagenes y adaptarlas a Glide u otro cargador. */
object ImageUtils {

    /** Convierte una ruta de imagen local o remota en un modelo valido para Glide. */
    fun toGlideModel(path: String?): Any? {
        if (path.isNullOrBlank()) return null

        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/uploads/") -> BackendConfig.currentBaseUrlWithoutTrailingSlash() + path
            path.startsWith("uploads/") -> "${BackendConfig.currentBaseUrlWithoutTrailingSlash()}/$path"
            else -> {
                val file = File(path)
                if (file.exists()) file else null
            }
        }
    }
}
