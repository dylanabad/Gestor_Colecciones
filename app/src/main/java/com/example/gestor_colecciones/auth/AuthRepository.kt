package com.example.gestor_colecciones.auth

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.AuthResponse
import com.example.gestor_colecciones.network.dto.LoginRequest
import com.example.gestor_colecciones.network.dto.RegisterRequest
import retrofit2.HttpException

class AuthRepository(
    private val api: ApiService,
    private val store: AuthStore
) {
    suspend fun login(email: String, password: String): AuthResponse {
        try {
            val response = api.login(LoginRequest(email = email, password = password))
            store.save(response.token, response.username, response.email)
            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResponse {
        try {
            val response = api.register(
                RegisterRequest(username = username, email = email, password = password)
            )
            store.save(response.token, response.username, response.email)
            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    fun hasSession(): Boolean = !store.getToken().isNullOrBlank()

    fun clearSession() = store.clear()

    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }
}