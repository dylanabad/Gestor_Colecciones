package com.example.gestor_colecciones.network

import android.content.Context
import com.example.gestor_colecciones.auth.AuthStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Construye y expone la instancia compartida de Retrofit para toda la aplicacion.
 *
 * Usa 10.0.2.2 porque, desde el emulador Android, esa direccion apunta al
 * localhost del equipo donde se ejecuta Spring Boot.
 */
object ApiProvider {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Volatile
    private var api: ApiService? = null

    /** Devuelve la instancia compartida de [ApiService]. */
    fun getApi(context: Context): ApiService {
        val currentApi = api
        if (currentApi != null) return currentApi

        return synchronized(this) {
            val alreadyBuilt = api
            if (alreadyBuilt != null) return@synchronized alreadyBuilt

            val authStore = AuthStore(context.applicationContext)
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(authStore))
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ApiService::class.java)
            api = service
            service
        }
    }

    /** Fuerza la recreacion del cliente en la siguiente llamada a [getApi]. */
    fun invalidate() {
        api = null
    }
}
