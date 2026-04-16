package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.UpdatePerfilRequest
import com.example.gestor_colecciones.network.dto.UsuarioPerfilDto
import okhttp3.MultipartBody

class PerfilRepository(
    private val api: ApiService
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
}

