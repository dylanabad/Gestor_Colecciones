package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Tag

// DTO que representa una etiqueta (tag) en la comunicación con la API
data class TagDto(
    val id: Long? = null,   // Identificador opcional del tag
    val nombre: String      // Nombre de la etiqueta
)

// Convierte DTO recibido desde la API a entidad local
fun TagDto.toEntity(): Tag {
    return Tag(
        id = id?.toInt() ?: 0, // Si no hay id, se asigna 0 por defecto
        nombre = nombre        // Nombre de la etiqueta sin cambios
    )
}

// Convierte entidad local a DTO para enviar a la API
fun Tag.toDto(): TagDto {
    return TagDto(
        id = id.takeIf { it > 0 }?.toLong(), // Solo envía el id si es válido
        nombre = nombre                      // Nombre del tag
    )
}