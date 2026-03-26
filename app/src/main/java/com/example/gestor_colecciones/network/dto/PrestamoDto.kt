package com.example.gestor_colecciones.network.dto

data class PrestamoDto(
    val movimientoId: Long,
    val itemId: Long,
    val itemTitulo: String,
    val itemImagenPath: String?,
    val propietarioId: Long,
    val propietarioUsername: String,
    val prestatarioId: Long,
    val prestatarioUsername: String,
    val fechaPrestamo: String,
    val fechaDevolucionPrevista: String?,
    val fechaDevolucionReal: String?,
    val estado: String,
    val notas: String?
)