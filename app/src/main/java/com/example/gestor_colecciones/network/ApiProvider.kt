package com.example.gestor_colecciones.network

import android.content.Context
import com.example.gestor_colecciones.auth.AuthStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiProvider {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Volatile
    private var api: ApiService? = null

    fun getApi(context: Context): ApiService {
        return api ?: synchronized(this) {
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
}
