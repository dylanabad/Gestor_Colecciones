package com.example.gestor_colecciones.network

import android.content.Context
import com.example.gestor_colecciones.auth.AuthStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/*
 * ApiProvider
 *
 * Singleton encargado de construir y mantener una única instancia de
 * `ApiService` (Retrofit) configurada con interceptores necesarios.
 *
 * - Define la `BASE_URL` de la API local (10.0.2.2 para emulador Android).
 * - Construye un OkHttpClient con un `AuthInterceptor` que añade token de
 *   autenticación desde `AuthStore` y un interceptor de logging para depuración.
 * - Crea el Retrofit y su `ApiService` una única vez (lazy + thread-safe).
 *
 * Nota: Solo se han añadido comentarios explicativos en español; no se modifica
 * la lógica de construcción del cliente ni del servicio.
 */
object ApiProvider {
    // URL base de la API (usar 10.0.2.2 para acceder al host desde el emulador Android)
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Instancia volátil de ApiService para acceso singleton thread-safe
    @Volatile
    private var api: ApiService? = null

    // Devuelve la instancia compartida de ApiService; la crea si no existe.
    // Se sincroniza sobre el objeto para asegurar que sólo se construye una vez
    // incluso en entornos multihilo.
    fun getApi(context: Context): ApiService {
        return api ?: synchronized(this) {
            // AuthStore para obtener el token actual de la sesión
            val authStore = AuthStore(context.applicationContext)

            // Interceptor de logging (útil en desarrollo para ver peticiones/respuestas)
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // OkHttp client con interceptor de autenticación y logging
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(authStore))
                .addInterceptor(logging)
                .build()

            // Construcción del cliente Retrofit con Gson como converter
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // Crear el servicio API y cachearlo en la variable `api`
            val service = retrofit.create(ApiService::class.java)
            api = service
            service
        }
    }
}
