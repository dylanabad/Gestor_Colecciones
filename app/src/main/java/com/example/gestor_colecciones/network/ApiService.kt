package com.example.gestor_colecciones.network

import com.example.gestor_colecciones.network.dto.AuthResponse
import com.example.gestor_colecciones.network.dto.CategoriaDto
import com.example.gestor_colecciones.network.dto.ColeccionDto
import com.example.gestor_colecciones.network.dto.ItemDeseoDto
import com.example.gestor_colecciones.network.dto.ItemDto
import com.example.gestor_colecciones.network.dto.LogroDto
import com.example.gestor_colecciones.network.dto.LoginRequest
import com.example.gestor_colecciones.network.dto.RegisterRequest
import com.example.gestor_colecciones.network.dto.TagDto
import com.example.gestor_colecciones.network.dto.UploadResponse
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.Query

/*
 * ApiService
 *
 * Interfaz Retrofit que define los endpoints remotos usados por la app.
 * Cada método corresponde a una llamada HTTP y utiliza DTOs (data transfer
 * objects) en los cuerpos y respuestas. Los métodos son `suspend` para poder
 * llamarlos desde coroutines.
 *
 * Las rutas están agrupadas por responsabilidad: autenticación, colecciones,
 * items, categorías, tags, deseos, uploads, logros, usuarios y préstamos.
 */
interface ApiService {

    // ── Autenticación ─────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // ── Colecciones ──────────────────────────────────────────────────────
    @GET("api/colecciones")
    suspend fun getColecciones(): List<ColeccionDto>

    @GET("api/colecciones/eliminadas")
    suspend fun getColeccionesEliminadas(): List<ColeccionDto>

    @POST("api/colecciones")
    suspend fun saveColeccion(@Body coleccion: ColeccionDto): ColeccionDto

    @GET("api/colecciones/{id}")
    suspend fun getColeccion(@Path("id") id: Long): ColeccionDto

    @DELETE("api/colecciones/{id}")
    suspend fun deleteColeccion(@Path("id") id: Long)

    @DELETE("api/colecciones/{id}/hard")
    suspend fun deleteColeccionHard(@Path("id") id: Long)

    // ── Items ───────────────────────────────────────────────────────────
    @GET("api/items/coleccion/{coleccionId}")
    suspend fun getItemsByColeccion(@Path("coleccionId") coleccionId: Long): List<ItemDto>

    @GET("api/items/coleccion/{coleccionId}/eliminados")
    suspend fun getItemsEliminadosByColeccion(@Path("coleccionId") coleccionId: Long): List<ItemDto>

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

    @DELETE("api/items/{id}/hard")
    suspend fun deleteItemHard(@Path("id") id: Long)

    // ── Categorías ──────────────────────────────────────────────────────
    @GET("api/categorias")
    suspend fun getCategorias(): List<CategoriaDto>

    @POST("api/categorias")
    suspend fun saveCategoria(@Body categoria: CategoriaDto): CategoriaDto

    @DELETE("api/categorias/{id}")
    suspend fun deleteCategoria(@Path("id") id: Long)

    // ── Tags ────────────────────────────────────────────────────────────
    @GET("api/tags")
    suspend fun getTags(): List<TagDto>

    @POST("api/tags")
    suspend fun saveTag(@Body tag: TagDto): TagDto

    @DELETE("api/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Long)

    @GET("api/items/{id}/tags")
    suspend fun getItemTags(@Path("id") itemId: Long): List<TagDto>

    @PUT("api/items/{id}/tags")
    suspend fun setItemTags(
        @Path("id") itemId: Long,
        @Body tagIds: List<Long>
    ): List<TagDto>

    // ── Deseos (wishlist) ──────────────────────────────────────────────
    @GET("api/deseos")
    suspend fun getDeseos(): List<ItemDeseoDto>

    @POST("api/deseos")
    suspend fun saveDeseo(@Body deseo: ItemDeseoDto): ItemDeseoDto

    @DELETE("api/deseos/{id}")
    suspend fun deleteDeseo(@Path("id") id: Long)

    @GET("api/deseos/eliminados")
    suspend fun getDeseosEliminados(): List<ItemDeseoDto>

    @DELETE("api/deseos/{id}/hard")
    suspend fun deleteDeseoHard(@Path("id") id: Long)

    // ── Uploads (subida de imágenes) ────────────────────────────────────
    @Multipart
    @POST("api/uploads")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResponse

    // ── Logros ──────────────────────────────────────────────────────────
    @GET("api/logros")
    suspend fun getLogros(): List<LogroDto>

    @POST("api/logros/{key}/unlock")
    suspend fun unlockLogro(@Path("key") key: String): LogroDto

    // ── Usuarios ────────────────────────────────────────────────────────
    @GET("api/usuarios")
    suspend fun getUsuarios(): List<UsuarioDto>

    // ── Préstamos ─────────────────────────────────────────────────────
    @POST("api/prestamos")
    suspend fun crearPrestamo(@Body request: PrestamoRequest): PrestamoDto

    @PUT("api/prestamos/{id}/devolver")
    suspend fun devolverPrestamo(@Path("id") id: Long): PrestamoDto

    @GET("api/prestamos/prestados")
    suspend fun getPrestados(): List<PrestamoDto>

    @GET("api/prestamos/recibidos")
    suspend fun getPrestamosRecibidos(): List<PrestamoDto>

    @DELETE("api/prestamos/{id}")
    suspend fun deletePrestamo(@Path("id") id: Long)
}
