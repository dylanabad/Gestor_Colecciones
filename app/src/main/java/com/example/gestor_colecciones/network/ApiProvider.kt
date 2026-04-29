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
 * La instancia se invalida automaticamente cuando cambia la URL base del backend.
 */
object ApiProvider {
    @Volatile
    private var api: ApiService? = null

    @Volatile
    private var configuredBaseUrl: String? = null

    /** Devuelve la instancia compartida de [ApiService], recreandola si cambia la URL base. */
    fun getApi(context: Context): ApiService {
        val desiredBaseUrl = BackendConfig.getBaseUrl(context)
        val currentApi = api
        if (currentApi != null && configuredBaseUrl == desiredBaseUrl) {
            return currentApi
        }

        return synchronized(this) {
            val alreadyBuilt = api
            if (alreadyBuilt != null && configuredBaseUrl == desiredBaseUrl) {
                return@synchronized alreadyBuilt
            }

            val authStore = AuthStore(context.applicationContext)
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(authStore))
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(desiredBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ApiService::class.java)
            configuredBaseUrl = desiredBaseUrl
            api = service
            service
        }
    }

    /** Fuerza la recreacion del cliente en la siguiente llamada a [getApi]. */
    fun invalidate() {
        api = null
        configuredBaseUrl = null
    }
}
