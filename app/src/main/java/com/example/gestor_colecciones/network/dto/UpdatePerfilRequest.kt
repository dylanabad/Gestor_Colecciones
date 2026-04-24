package com.example.gestor_colecciones.network.dto

// Request para actualizar el perfil del usuario autenticado.
/**
 * Payload usado para persistir cambios editables del perfil de usuario.
 */
data class UpdatePerfilRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null
)

