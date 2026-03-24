package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.DateMapper
import java.util.Date

data class ItemDto(
    val id: Long? = null,
    val titulo: String,
    val valor: Double? = 0.0,
    val fechaAdquisicion: String? = null,
    val imagenPath: String? = null,
    val estado: String? = null,
    val descripcion: String? = null,
    val calificacion: Float? = 0f,
    val eliminado: Boolean? = false,
    val fechaEliminacion: String? = null,
    val categoria: CategoriaDto? = null
)

fun ItemDto.toEntity(collectionId: Int): Item {
    return Item(
        id = id?.toInt() ?: 0,
        titulo = titulo,
        categoriaId = categoria?.id?.toInt() ?: 0,
        collectionId = collectionId,
        fechaAdquisicion = DateMapper.parse(fechaAdquisicion) ?: Date(),
        valor = valor ?: 0.0,
        imagenPath = imagenPath,
        estado = estado.orEmpty(),
        descripcion = descripcion,
        calificacion = calificacion ?: 0f,
        eliminado = eliminado ?: false,
        fechaEliminacion = DateMapper.parse(fechaEliminacion)
    )
}

fun Item.toDto(): ItemDto {
    return ItemDto(
        id = id.takeIf { it > 0 }?.toLong(),
        titulo = titulo,
        valor = valor,
        fechaAdquisicion = DateMapper.format(fechaAdquisicion),
        imagenPath = imagenPath,
        estado = estado,
        descripcion = descripcion,
        calificacion = calificacion,
        eliminado = eliminado,
        fechaEliminacion = DateMapper.format(fechaEliminacion)
    )
}
