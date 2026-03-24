package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.network.DateMapper

data class ItemDeseoDto(
    val id: Long? = null,
    val titulo: String,
    val descripcion: String? = null,
    val precioObjetivo: Double? = 0.0,
    val prioridad: Int? = 2,
    val enlace: String? = null,
    val conseguido: Boolean? = false,
    val fechaCreacion: String? = null,
    val fechaConseguido: String? = null
)

fun ItemDeseoDto.toEntity(): ItemDeseo {
    return ItemDeseo(
        id = id?.toInt() ?: 0,
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

fun ItemDeseo.toDto(): ItemDeseoDto {
    return ItemDeseoDto(
        id = id.takeIf { it > 0 }?.toLong(),
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
