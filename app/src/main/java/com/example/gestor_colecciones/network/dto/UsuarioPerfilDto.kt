package com.example.gestor_colecciones.network.dto

// DTO de perfil del usuario autenticado (coleccionista)
data class UsuarioPerfilDto(
    val id: Long,
    val username: String,
    val email: String,
    val fechaCreacion: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null
)

