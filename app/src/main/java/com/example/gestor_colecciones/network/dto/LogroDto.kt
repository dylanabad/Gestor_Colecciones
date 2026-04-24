package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.network.DateMapper

/**
 * DTO que modela el estado remoto de un logro desbloqueable.
 */
data class LogroDto(
    val key: String,                     // Identificador único del logro
    val desbloqueado: Boolean = false,   // Indica si el logro está desbloqueado
    val fechaDesbloqueo: String? = null  // Fecha en la que se desbloqueó (formato String API)
)

// Convierte DTO recibido desde la API a entidad local
fun LogroDto.toEntity(): Logro {
    return Logro(
        key = key,
        desbloqueado = desbloqueado,
        fechaDesbloqueo = DateMapper.parse(fechaDesbloqueo)
    )
}