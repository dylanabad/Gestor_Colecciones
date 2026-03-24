package com.example.gestor_colecciones.network.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val username: String,
    val email: String
)
