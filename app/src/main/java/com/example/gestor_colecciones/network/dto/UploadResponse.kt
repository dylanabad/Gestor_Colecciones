package com.example.gestor_colecciones.network.dto

/**
 * Respuesta del endpoint de subida de imagenes.
 */
data class UploadResponse(
    val url: String // URL del archivo subido devuelta por la API
)