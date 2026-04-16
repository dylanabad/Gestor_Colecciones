package com.example.gestor_colecciones.network.dto

// DTO que representa un préstamo en la comunicación con la API
data class PrestamoDto(
    val movimientoId: Long,                    // Identificador del movimiento del préstamo
    val itemId: Long,                          // ID del item prestado
    val itemTitulo: String,                    // Título del item prestado
    val itemImagenPath: String?,               // Ruta de imagen del item (opcional)
    val propietarioId: Long,                   // ID del usuario propietario del item
    val propietarioUsername: String,           // Username del propietario
    val propietarioAvatarPath: String? = null, // Avatar del propietario (opcional)
    val prestatarioId: Long,                   // ID del usuario que recibe el préstamo
    val prestatarioUsername: String,           // Username del prestatario
    val prestatarioAvatarPath: String? = null, // Avatar del prestatario (opcional)
    val fechaPrestamo: String,                 // Fecha en la que se realizó el préstamo
    val fechaDevolucionPrevista: String?,      // Fecha prevista de devolución (opcional)
    val fechaDevolucionReal: String?,          // Fecha real de devolución (opcional)
    val estado: String,                        // Estado del préstamo (ACTIVO/DEVUELTO/etc.)
    val notas: String?                         // Notas adicionales del préstamo
)

