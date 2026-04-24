package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.UsuarioPerfilDto
import com.example.gestor_colecciones.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/** Estado operativo de la pantalla de perfil. */
sealed class PerfilState {
    object Idle : PerfilState()
    object Loading : PerfilState()
    data class Error(val message: String) : PerfilState()
    data class Saved(val message: String) : PerfilState()
}

/**
 * Gestiona la carga y actualizacion del perfil del coleccionista.
 *
 * Separa la informacion del perfil, las estadisticas derivadas y el estado de
 * guardado para que la UI pueda reaccionar de forma independiente a cada flujo.
 */
class PerfilViewModel(
    private val repository: PerfilRepository
) : ViewModel() {

    /** Perfil remoto actual del usuario autenticado. */
    private val _perfil = MutableStateFlow<UsuarioPerfilDto?>(null)
    val perfil: StateFlow<UsuarioPerfilDto?> = _perfil

    /** Estadisticas agregadas que se muestran en el encabezado del perfil. */
    private val _stats = MutableStateFlow<PerfilRepository.Stats?>(null)
    val stats: StateFlow<PerfilRepository.Stats?> = _stats

    /** Estado general de carga y guardado. */
    private val _state = MutableStateFlow<PerfilState>(PerfilState.Idle)
    val state: StateFlow<PerfilState> = _state

    /** Carga de forma conjunta el perfil remoto y las estadisticas locales. */
    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = PerfilState.Loading
            try {
                _perfil.value = repository.getMiPerfil()
                _stats.value = repository.getStats()
                _state.value = PerfilState.Idle
            } catch (e: Exception) {
                _state.value = PerfilState.Error(e.message ?: "Error cargando perfil")
            }
        }
    }

    /** Guarda cambios textuales del perfil y, opcionalmente, sube o elimina el avatar. */
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

    /** Limpia el estado temporal una vez consumido por la UI. */
    fun resetState() {
        _state.value = PerfilState.Idle
    }
}
