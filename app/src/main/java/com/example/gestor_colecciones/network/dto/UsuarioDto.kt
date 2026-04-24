package com.example.gestor_colecciones.network.dto

/**
 * Representa un usuario visible como destinatario o referencia en modulos sociales.
 */
data class UsuarioDto(
    val id: Long,        // Identificador único del usuario
    val username: String,// Nombre de usuario
    val email: String    // Correo electrónico del usuario
)