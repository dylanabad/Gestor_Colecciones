package com.example.gestor_colecciones.network

import com.example.gestor_colecciones.auth.AuthStore
import okhttp3.Interceptor
import okhttp3.Response

/*
 * AuthInterceptor.kt
 *
 * Interceptor OkHttp que añade el encabezado de autorización (Bearer token)
 * a las peticiones salientes cuando exista un token válido en `AuthStore`.
 *
 * Este interceptor se inyecta en el cliente OkHttp usado por Retrofit para
 * garantizar que todas las llamadas remotas que requieran autenticación
 * envíen el token en la cabecera `Authorization`.
 */
class AuthInterceptor(private val authStore: AuthStore) : Interceptor {
    // Intercepta la petición, comprueba si hay token y añade la cabecera si procede
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Obtener token desde el almacén de autenticación (puede ser null o vacío)
        val token = authStore.getToken()
        // Si no hay token, proceder con la petición original sin modificar
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            // Añadir cabecera Authorization: Bearer <token>
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        // Continuar la cadena de interceptores con la petición (modificada o no)
        return chain.proceed(request)
    }
}
