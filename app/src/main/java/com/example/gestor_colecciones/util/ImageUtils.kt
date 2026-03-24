package com.example.gestor_colecciones.util

import java.io.File

object ImageUtils {
    private const val BASE_URL = "http://10.0.2.2:8080"

    fun toGlideModel(path: String?): Any? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/uploads/") -> BASE_URL + path
            path.startsWith("uploads/") -> "$BASE_URL/$path"
            else -> {
                val file = File(path)
                if (file.exists()) file else null
            }
        }
    }
}
