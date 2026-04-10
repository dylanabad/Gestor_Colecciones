package com.example.gestor_colecciones.network.dto

// DTO usado para crear un nuevo préstamo en la API
data class PrestamoRequest(
    val itemId: Long,                         // ID del item que se va a prestar
    val prestatarioUsuarioId: Long,           // ID del usuario que recibirá el préstamo
    val fechaDevolucionPrevista: String? = null, // Fecha prevista de devolución (opcional)
    val notas: String? = null                 // Notas adicionales opcionales sobre el préstamo
)