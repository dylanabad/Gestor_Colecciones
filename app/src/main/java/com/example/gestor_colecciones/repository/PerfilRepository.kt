package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.UpdatePerfilRequest
import com.example.gestor_colecciones.network.dto.UsuarioPerfilDto
import okhttp3.MultipartBody

/**
 * Repositorio del perfil de usuario.
 *
 * Combina informacion remota del backend con estadisticas agregadas calculadas
 * desde Room para que la pantalla de perfil no tenga que conocer ambos origenes.
 */
class PerfilRepository(
    private val api: ApiService,
    private val coleccionDao: com.example.gestor_colecciones.dao.ColeccionDao,
    private val itemDao: com.example.gestor_colecciones.dao.ItemDao,
    private val logroDao: com.example.gestor_colecciones.dao.LogroDao
) {
    /** Recupera el perfil remoto del usuario autenticado. */
    suspend fun getMiPerfil(): UsuarioPerfilDto =
        api.getMiPerfil()

    /** Persiste en backend los cambios editables del perfil. */
    suspend fun updateMiPerfil(
        displayName: String?,
        bio: String?,
        avatarPath: String?
    ): UsuarioPerfilDto =
        api.updateMiPerfil(
            UpdatePerfilRequest(
                displayName = displayName,
                bio = bio,
                avatarPath = avatarPath
            )
        )

    /** Sube el avatar al backend y devuelve la ruta remota persistida. */
    suspend fun uploadAvatar(part: MultipartBody.Part): String =
        api.uploadImage(part).url

    /** Calcula las estadisticas locales que enriquecen la pantalla de perfil. */
    suspend fun getStats(): Stats {
        val collections = coleccionDao.countColecciones()
        val items = itemDao.getTotalItems()
        val totalValue = itemDao.getTotalValor() ?: 0.0
        val logros = logroDao.countDesbloqueados()
        return Stats(collections, items, totalValue, logros)
    }

    /** Resumen agregado mostrado en el encabezado del perfil. */
    data class Stats(
        val collections: Int,
        val items: Int,
        val totalValue: Double,
        val logros: Int
    )
}
