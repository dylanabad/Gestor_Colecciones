package com.example.gestor_colecciones.network.dto

/**
 * Cuerpo enviado al backend para autenticacion por email y contrasena.
 */
data class LoginRequest(
    val email: String,     // Email del usuario para autenticación
    val password: String   // Contraseña del usuario
)

/**
 * Cuerpo usado en el login estricto con usuario, email y contrasena.
 */
data class LoginStrictRequest(
    val username: String,
    val email: String,
    val password: String
)

/**
 * Cuerpo enviado al backend para registrar un nuevo usuario.
 */
data class RegisterRequest(
    val username: String,  // Nombre de usuario elegido
    val email: String,     // Email del nuevo usuario
    val password: String   // Contraseña del nuevo usuario
)

/**
 * Respuesta de autenticacion devuelta por el backend tras login o registro.
 */
data class AuthResponse(
    val token: String,     // Token JWT o similar para sesiones autenticadas
    val username: String,  // Nombre de usuario devuelto por el backend
    val email: String      // Email confirmado del usuario autenticado
)
