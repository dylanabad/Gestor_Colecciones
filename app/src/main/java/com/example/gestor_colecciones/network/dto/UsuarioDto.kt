package com.example.gestor_colecciones.network.dto

// DTO que representa un usuario en la comunicación con la API
data class UsuarioDto(
    val id: Long,        // Identificador único del usuario
    val username: String,// Nombre de usuario
    val email: String    // Correo electrónico del usuario
)