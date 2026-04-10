package com.example.gestor_colecciones.util

import java.io.File

// Utilidad para normalizar rutas de imágenes y adaptarlas a Glide u otro cargador
object ImageUtils {

    // URL base del backend (emulador Android -> localhost del PC)
    private const val BASE_URL = "http://10.0.2.2:8080"

    // Convierte una ruta de imagen (local o remota) a un modelo válido para Glide
    fun toGlideModel(path: String?): Any? {

        // Si no hay ruta, no hay imagen
        if (path.isNullOrBlank()) return null

        return when {

            // Si ya es una URL completa, se usa directamente
            path.startsWith("http://") || path.startsWith("https://") -> path

            // Si viene como /uploads/archivo.jpg, se concatena con el servidor
            path.startsWith("/uploads/") -> BASE_URL + path

            // Si viene como uploads/archivo.jpg, se añade la base con slash
            path.startsWith("uploads/") -> "$BASE_URL/$path"

            // Si es una ruta local del dispositivo, se intenta usar como archivo
            else -> {
                val file = File(path)

                // Solo se devuelve si el archivo existe
                if (file.exists()) file else null
            }
        }
    }
}