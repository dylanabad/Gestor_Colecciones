package com.example.gestor_colecciones.network.dto

// Request para actualizar el perfil del usuario autenticado.
// Campos null no se modifican.
data class UpdatePerfilRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null
)

