package com.example.gestor_colecciones.network.dto

// DTO usado para enviar credenciales de login al servidor
data class LoginRequest(
    val email: String,     // Email del usuario para autenticación
    val password: String   // Contraseña del usuario
)

// DTO usado para enviar datos de registro al servidor
data class RegisterRequest(
    val username: String,  // Nombre de usuario elegido
    val email: String,     // Email del nuevo usuario
    val password: String   // Contraseña del nuevo usuario
)

// DTO que representa la respuesta de autenticación del servidor
data class AuthResponse(
    val token: String,     // Token JWT o similar para sesiones autenticadas
    val username: String,  // Nombre de usuario devuelto por el backend
    val email: String      // Email confirmado del usuario autenticado
)