package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.network.DateMapper
import java.util.Date

data class ColeccionDto(
    val id: Long? = null,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: String? = null,
    val imagenPath: String? = null,
    val color: Int? = 0,
    val itemsCount: Int? = 0,
    val totalValue: Double? = 0.0,
    val eliminado: Boolean? = false,
    val fechaEliminacion: String? = null
)

fun ColeccionDto.toEntity(): Coleccion {
    return Coleccion(
        id = id?.toInt() ?: 0,
        nombre = nombre,
        descripcion = descripcion,
        fechaCreacion = DateMapper.parse(fechaCreacion) ?: Date(),
        imagenPath = imagenPath,
        color = color ?: 0,
        itemsCount = itemsCount ?: 0,
        totalValue = totalValue ?: 0.0,
        eliminado = eliminado ?: false,
        fechaEliminacion = DateMapper.parse(fechaEliminacion)
    )
}

fun Coleccion.toDto(): ColeccionDto {
    return ColeccionDto(
        id = id.takeIf { it > 0 }?.toLong(),
        nombre = nombre,
        descripcion = descripcion,
        fechaCreacion = DateMapper.format(fechaCreacion),
        imagenPath = imagenPath,
        color = color,
        itemsCount = itemsCount,
        totalValue = totalValue,
        eliminado = eliminado,
        fechaEliminacion = DateMapper.format(fechaEliminacion)
    )
}
