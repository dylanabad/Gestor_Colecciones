package com.example.gestor_colecciones.network.dto

data class PrestamoRequest(
    val itemId: Long,
    val prestatarioUsuarioId: Long,
    val fechaDevolucionPrevista: String? = null,
    val notas: String? = null
)