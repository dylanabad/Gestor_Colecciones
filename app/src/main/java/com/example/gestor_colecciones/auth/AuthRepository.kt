package com.example.gestor_colecciones.auth

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.AuthResponse
import com.example.gestor_colecciones.network.dto.LoginRequest
import com.example.gestor_colecciones.network.dto.RegisterRequest
import retrofit2.HttpException

// Repositorio encargado de la autenticación (login, registro y gestión de sesión)
class AuthRepository(
    private val api: ApiService,   // Servicio API para llamadas de red
    private val store: AuthStore   // Almacenamiento local de la sesión
) {

    // Realiza login con email y password
    suspend fun login(email: String, password: String): AuthResponse {
        try {
            val response = api.login(
                LoginRequest(email = email, password = password)
            )

            // Guarda la sesión obtenida
            store.save(response.token, response.username, response.email)

            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    // Registra un nuevo usuario
    suspend fun register(username: String, email: String, password: String): AuthResponse {
        try {
            val response = api.register(
                RegisterRequest(
                    username = username,
                    email = email,
                    password = password
                )
            )

            // Guarda la sesión tras registro exitoso
            store.save(response.token, response.username, response.email)

            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    // Comprueba si hay una sesión activa
    fun hasSession(): Boolean =
        !store.getToken().isNullOrBlank()

    // Borra la sesión actual
    fun clearSession() =
        store.clear()

    // Extrae un mensaje de error legible desde una excepción HTTP
    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank())
            "HTTP ${e.code()}"
        else
            body
    }
}