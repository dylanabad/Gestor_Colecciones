package com.example.gestor_colecciones.network.dto

/**
 * Modelo remoto del perfil publico o privado del usuario autenticado.
 */
data class UsuarioPerfilDto(
    val id: Long,
    val username: String,
    val email: String,
    val fechaCreacion: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null
)

