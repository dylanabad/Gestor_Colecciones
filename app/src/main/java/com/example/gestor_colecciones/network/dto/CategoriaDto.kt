package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Categoria

/**
 * Representacion remota de una categoria y sus conversiones a entidad local.
 */
data class CategoriaDto(
    val id: Long? = null,  // Identificador opcional (puede venir nulo en creación)
    val nombre: String     // Nombre de la categoría
)

// Convierte un DTO de categoría a entidad de base de datos/local
fun CategoriaDto.toEntity(): Categoria {
    return Categoria(
        id = id?.toInt() ?: 0, // Si no hay id, se usa 0 como valor por defecto
        nombre = nombre        // Se mantiene el nombre sin cambios
    )
}

// Convierte una entidad local a DTO para enviar a la API
fun Categoria.toDto(): CategoriaDto {
    return CategoriaDto(
        id = id.takeIf { it > 0 }?.toLong(), // Solo envía el id si es válido (>0)
        nombre = nombre                      // Nombre de la categoría
    )
}