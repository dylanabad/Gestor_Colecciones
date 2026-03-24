package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Tag

data class TagDto(
    val id: Long? = null,
    val nombre: String
)

fun TagDto.toEntity(): Tag {
    return Tag(
        id = id?.toInt() ?: 0,
        nombre = nombre
    )
}

fun Tag.toDto(): TagDto {
    return TagDto(
        id = id.takeIf { it > 0 }?.toLong(),
        nombre = nombre
    )
}
