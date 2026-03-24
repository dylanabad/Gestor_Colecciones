package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Categoria

data class CategoriaDto(
    val id: Long? = null,
    val nombre: String
)

fun CategoriaDto.toEntity(): Categoria {
    return Categoria(
        id = id?.toInt() ?: 0,
        nombre = nombre
    )
}

fun Categoria.toDto(): CategoriaDto {
    return CategoriaDto(
        id = id.takeIf { it > 0 }?.toLong(),
        nombre = nombre
    )
}
