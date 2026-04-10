package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.network.DateMapper

// DTO que representa un item de deseo en la comunicación con la API
data class ItemDeseoDto(
    val id: Long? = null,                  // Identificador opcional del item
    val titulo: String,                    // Título del item de deseo
    val descripcion: String? = null,       // Descripción opcional
    val precioObjetivo: Double? = 0.0,     // Precio objetivo deseado
    val prioridad: Int? = 2,               // Prioridad (por defecto media = 2)
    val enlace: String? = null,            // Enlace externo relacionado al item
    val conseguido: Boolean? = false,      // Indica si ya se ha conseguido
    val fechaCreacion: String? = null,     // Fecha de creación en formato String (API)
    val fechaConseguido: String? = null    // Fecha en la que se consiguió el item
)

// Convierte DTO recibido desde la API a entidad local
fun ItemDeseoDto.toEntity(): ItemDeseo {
    return ItemDeseo(
        id = id?.toInt() ?: 0, // Si no hay id, se asigna 0
        titulo = titulo,
        descripcion = descripcion,
        precioObjetivo = precioObjetivo ?: 0.0,
        prioridad = prioridad ?: 2,
        enlace = enlace,
        conseguido = conseguido ?: false,
        fechaCreacion = DateMapper.parse(fechaCreacion) ?: java.util.Date(),
        fechaConseguido = DateMapper.parse(fechaConseguido)
    )
}

// Convierte entidad local a DTO para enviar a la API
fun ItemDeseo.toDto(): ItemDeseoDto {
    return ItemDeseoDto(
        id = id.takeIf { it > 0 }?.toLong(), // Solo envía id si es válido
        titulo = titulo,
        descripcion = descripcion,
        precioObjetivo = precioObjetivo,
        prioridad = prioridad,
        enlace = enlace,
        conseguido = conseguido,
        fechaCreacion = DateMapper.format(fechaCreacion),
        fechaConseguido = DateMapper.format(fechaConseguido)
    )
}