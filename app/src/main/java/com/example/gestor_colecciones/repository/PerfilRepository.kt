package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.UpdatePerfilRequest
import com.example.gestor_colecciones.network.dto.UsuarioPerfilDto
import okhttp3.MultipartBody

class PerfilRepository(
    private val api: ApiService,
    private val coleccionDao: com.example.gestor_colecciones.dao.ColeccionDao,
    private val itemDao: com.example.gestor_colecciones.dao.ItemDao,
    private val logroDao: com.example.gestor_colecciones.dao.LogroDao
) {
    suspend fun getMiPerfil(): UsuarioPerfilDto =
        api.getMiPerfil()

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

    suspend fun uploadAvatar(part: MultipartBody.Part): String =
        api.uploadImage(part).url

    suspend fun getStats(): Stats {
        val collections = coleccionDao.countColecciones()
        val items = itemDao.getTotalItems()
        val totalValue = itemDao.getTotalValor() ?: 0.0
        val logros = logroDao.countDesbloqueados()
        return Stats(collections, items, totalValue, logros)
    }

    data class Stats(
        val collections: Int,
        val items: Int,
        val totalValue: Double,
        val logros: Int
    )
}

