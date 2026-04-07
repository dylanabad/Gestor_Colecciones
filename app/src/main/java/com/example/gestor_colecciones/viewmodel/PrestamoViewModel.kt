package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import com.example.gestor_colecciones.repository.PrestamoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PrestamoState {
    object Idle : PrestamoState()
    object Loading : PrestamoState()
    data class Success(val message: String) : PrestamoState()
    data class Error(val message: String) : PrestamoState()
}

class PrestamoViewModel(private val repository: PrestamoRepository) : ViewModel() {

    private val _prestados = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val prestados: StateFlow<List<PrestamoDto>> = _prestados

    private val _recibidos = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val recibidos: StateFlow<List<PrestamoDto>> = _recibidos

    private val _usuarios = MutableStateFlow<List<UsuarioDto>>(emptyList())
    val usuarios: StateFlow<List<UsuarioDto>> = _usuarios

    private val _state = MutableStateFlow<PrestamoState>(PrestamoState.Idle)
    val state: StateFlow<PrestamoState> = _state

    fun cargarPrestados() {
        viewModelScope.launch {
            try {
                _prestados.value = repository.getPrestados()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando préstamos: ${e.message}")
            }
        }
    }

    fun cargarRecibidos() {
        viewModelScope.launch {
            try {
                _recibidos.value = repository.getPrestamosRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando préstamos recibidos: ${e.message}")
            }
        }
    }

    fun cargarUsuarios() {
        viewModelScope.launch {
            try {
                _usuarios.value = repository.getUsuarios()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando usuarios: ${e.message}")
            }
        }
    }

    fun crearPrestamo(request: PrestamoRequest) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.crearPrestamo(request)
                _state.value = PrestamoState.Success("Préstamo creado correctamente")
                // Refrescamos ambas listas para que tanto prestador como destinatario vean la actualización
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al crear el préstamo")
            }
        }
    }

    fun devolverPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.devolverPrestamo(id)
                _state.value = PrestamoState.Success("Devolución registrada correctamente")
                // Refrescamos ambas listas tras la devolución
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al registrar la devolución")
            }
        }
    }

    fun eliminarPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.deletePrestamo(id)
                _state.value = PrestamoState.Success("Préstamo eliminado correctamente")
                // Refrescamos listas
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al eliminar el préstamo")
            }
        }
    }

    fun resetState() {
        _state.value = PrestamoState.Idle
    }
}