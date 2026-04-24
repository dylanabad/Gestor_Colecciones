package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.network.DateMapper
import java.util.Date

/**
 * Representacion remota de una coleccion y sus adaptadores con Room.
 */
data class ColeccionDto(
    val id: Long? = null,                 // Identificador opcional (puede no venir del servidor)
    val nombre: String,                   // Nombre de la colección
    val descripcion: String? = null,     // Descripción opcional
    val fechaCreacion: String? = null,   // Fecha de creación en formato String (API)
    val imagenPath: String? = null,      // Ruta de imagen asociada a la colección
    val color: Int? = 0,                 // Color asociado (por defecto 0 si no viene)
    val itemsCount: Int? = 0,            // Número de items dentro de la colección
    val totalValue: Double? = 0.0,       // Valor total de la colección
    val eliminado: Boolean? = false,     // Flag de eliminación lógica
    val fechaEliminacion: String? = null // Fecha de eliminación en formato String (API)
)

// Convierte DTO recibido desde la API a entidad local
fun ColeccionDto.toEntity(): Coleccion {
    return Coleccion(
        id = id?.toInt() ?: 0, // Si no hay id se asigna 0
        nombre = nombre,
        descripcion = descripcion,
        fechaCreacion = DateMapper.parse(fechaCreacion) ?: Date(), // Convierte String a Date o usa fecha actual
        imagenPath = imagenPath,
        color = color ?: 0,
        itemsCount = itemsCount ?: 0,
        totalValue = totalValue ?: 0.0,
        eliminado = eliminado ?: false,
        fechaEliminacion = DateMapper.parse(fechaEliminacion) // Puede ser null si no existe
    )
}

// Convierte entidad local a DTO para enviar a la API
fun Coleccion.toDto(): ColeccionDto {
    return ColeccionDto(
        id = id.takeIf { it > 0 }?.toLong(), // Solo envía id si es válido
        nombre = nombre,
        descripcion = descripcion,
        fechaCreacion = DateMapper.format(fechaCreacion), // Convierte Date a String
        imagenPath = imagenPath,
        color = color,
        itemsCount = itemsCount,
        totalValue = totalValue,
        eliminado = eliminado,
        fechaEliminacion = DateMapper.format(fechaEliminacion) // Convierte Date a String
    )
}