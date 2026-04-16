package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.UsuarioPerfilDto
import com.example.gestor_colecciones.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

sealed class PerfilState {
    object Idle : PerfilState()
    object Loading : PerfilState()
    data class Error(val message: String) : PerfilState()
    data class Saved(val message: String) : PerfilState()
}

class PerfilViewModel(
    private val repository: PerfilRepository
) : ViewModel() {

    private val _perfil = MutableStateFlow<UsuarioPerfilDto?>(null)
    val perfil: StateFlow<UsuarioPerfilDto?> = _perfil

    private val _state = MutableStateFlow<PerfilState>(PerfilState.Idle)
    val state: StateFlow<PerfilState> = _state

    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = PerfilState.Loading
            try {
                _perfil.value = repository.getMiPerfil()
                _state.value = PerfilState.Idle
            } catch (e: Exception) {
                _state.value = PerfilState.Error(e.message ?: "Error cargando perfil")
            }
        }
    }

    fun guardarCambios(
        displayName: String?,
        bio: String?,
        avatarPart: MultipartBody.Part?,
        removeAvatar: Boolean
    ) {
        viewModelScope.launch {
            _state.value = PerfilState.Loading
            try {
                val avatarPath = when {
                    removeAvatar -> ""
                    avatarPart != null -> repository.uploadAvatar(avatarPart)
                    else -> null
                }

                val updated = repository.updateMiPerfil(
                    displayName = displayName,
                    bio = bio,
                    avatarPath = avatarPath
                )
                _perfil.value = updated
                _state.value = PerfilState.Saved("Perfil actualizado")
            } catch (e: Exception) {
                _state.value = PerfilState.Error(e.message ?: "Error guardando perfil")
            }
        }
    }

    fun resetState() {
        _state.value = PerfilState.Idle
    }
}

