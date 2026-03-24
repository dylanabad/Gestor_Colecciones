package com.example.gestor_colecciones.network

import com.example.gestor_colecciones.auth.AuthStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val authStore: AuthStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authStore.getToken()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
