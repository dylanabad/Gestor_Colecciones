package com.example.gestor_colecciones.network.dto

/**
 * Cuerpo enviado al backend para crear un nuevo prestamo.
 */
data class PrestamoRequest(
    val itemId: Long,                         // ID del item que se va a prestar
    val prestatarioUsuarioId: Long,           // ID del usuario que recibirá el préstamo
    val fechaDevolucionPrevista: String? = null, // Fecha prevista de devolución (opcional)
    val notas: String? = null                 // Notas adicionales opcionales sobre el préstamo
)