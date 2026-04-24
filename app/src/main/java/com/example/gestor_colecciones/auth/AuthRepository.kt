package com.example.gestor_colecciones.auth

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.AuthResponse
import com.example.gestor_colecciones.network.dto.LoginRequest
import com.example.gestor_colecciones.network.dto.LoginStrictRequest
import com.example.gestor_colecciones.network.dto.RegisterRequest
import retrofit2.HttpException

/**
 * Centraliza la autenticacion contra el backend y la persistencia local
 * de la sesion actual.
 *
 * La clase encapsula login, registro y la escritura del token JWT junto con
 * los datos basicos del usuario en [AuthStore].
 */
class AuthRepository(
    private val api: ApiService,
    private val store: AuthStore
) {

    /**
     * Autentica al usuario mediante email y contrasena.
     *
     * Si la peticion se completa correctamente, guarda la sesion antes de
     * devolver la respuesta a la UI.
     */
    suspend fun login(email: String, password: String): AuthResponse {
        try {
            val response = api.login(
                LoginRequest(email = email, password = password)
            )
            store.save(response.token, response.username, response.email)
            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    /** Ejecuta el login estricto validando usuario, email y contrasena. */
    suspend fun loginStrict(username: String, email: String, password: String): AuthResponse {
        try {
            val response = api.loginStrict(
                LoginStrictRequest(username = username, email = email, password = password)
            )
            store.save(response.token, response.username, response.email)
            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    /**
     * Registra un nuevo usuario y abre sesion automaticamente con la respuesta
     * devuelta por el backend.
     */
    suspend fun register(username: String, email: String, password: String): AuthResponse {
        try {
            val response = api.register(
                RegisterRequest(
                    username = username,
                    email = email,
                    password = password
                )
            )
            store.save(response.token, response.username, response.email)
            return response
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    /** Indica si existe una sesion valida almacenada localmente. */
    fun hasSession(): Boolean =
        !store.getToken().isNullOrBlank()

    /** Elimina por completo la sesion actual del almacenamiento local. */
    fun clearSession() =
        store.clear()

    /** Convierte una [HttpException] en un mensaje consumible por la capa de UI. */
    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }
}
