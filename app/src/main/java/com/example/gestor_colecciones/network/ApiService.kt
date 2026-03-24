package com.example.gestor_colecciones.network

import com.example.gestor_colecciones.network.dto.AuthResponse
import com.example.gestor_colecciones.network.dto.CategoriaDto
import com.example.gestor_colecciones.network.dto.ColeccionDto
import com.example.gestor_colecciones.network.dto.ItemDto
import com.example.gestor_colecciones.network.dto.LogroDto
import com.example.gestor_colecciones.network.dto.LoginRequest
import com.example.gestor_colecciones.network.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("api/colecciones")
    suspend fun getColecciones(): List<ColeccionDto>

    @POST("api/colecciones")
    suspend fun saveColeccion(@Body coleccion: ColeccionDto): ColeccionDto

    @GET("api/colecciones/{id}")
    suspend fun getColeccion(@Path("id") id: Long): ColeccionDto

    @DELETE("api/colecciones/{id}")
    suspend fun deleteColeccion(@Path("id") id: Long)

    @GET("api/items/coleccion/{coleccionId}")
    suspend fun getItemsByColeccion(@Path("coleccionId") coleccionId: Long): List<ItemDto>

    @POST("api/items/coleccion/{coleccionId}")
    suspend fun saveItem(
        @Path("coleccionId") coleccionId: Long,
        @Query("categoriaId") categoriaId: Long?,
        @Body item: ItemDto
    ): ItemDto

    @GET("api/items/{id}")
    suspend fun getItem(@Path("id") id: Long): ItemDto

    @DELETE("api/items/{id}")
    suspend fun deleteItem(@Path("id") id: Long)

    @GET("api/categorias")
    suspend fun getCategorias(): List<CategoriaDto>

    @POST("api/categorias")
    suspend fun saveCategoria(@Body categoria: CategoriaDto): CategoriaDto

    @DELETE("api/categorias/{id}")
    suspend fun deleteCategoria(@Path("id") id: Long)

    @GET("api/logros")
    suspend fun getLogros(): List<LogroDto>

    @POST("api/logros/{key}/unlock")
    suspend fun unlockLogro(@Path("key") key: String): LogroDto
}
