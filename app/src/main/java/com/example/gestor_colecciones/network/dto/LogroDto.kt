package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.network.DateMapper

data class LogroDto(
    val key: String,
    val desbloqueado: Boolean = false,
    val fechaDesbloqueo: String? = null
)

fun LogroDto.toEntity(): Logro {
    return Logro(
        key = key,
        desbloqueado = desbloqueado,
        fechaDesbloqueo = DateMapper.parse(fechaDesbloqueo)
    )
}
