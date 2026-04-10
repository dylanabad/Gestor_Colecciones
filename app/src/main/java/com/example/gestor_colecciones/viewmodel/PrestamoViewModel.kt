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

// Estados posibles del módulo de préstamos
sealed class PrestamoState {
    object Idle : PrestamoState()
    object Loading : PrestamoState()
    data class Success(val message: String) : PrestamoState()
    data class Error(val message: String) : PrestamoState()
}

// ViewModel encargado de gestionar préstamos (API o backend)
class PrestamoViewModel(
    private val repository: PrestamoRepository
) : ViewModel() {

    // Lista de préstamos enviados por el usuario
    private val _prestados = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val prestados: StateFlow<List<PrestamoDto>> = _prestados

    // Lista de préstamos recibidos por el usuario
    private val _recibidos = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val recibidos: StateFlow<List<PrestamoDto>> = _recibidos

    // Lista de usuarios disponibles para prestar
    private val _usuarios = MutableStateFlow<List<UsuarioDto>>(emptyList())
    val usuarios: StateFlow<List<UsuarioDto>> = _usuarios

    // Estado general de operaciones (loading, error, success, idle)
    private val _state = MutableStateFlow<PrestamoState>(PrestamoState.Idle)
    val state: StateFlow<PrestamoState> = _state

    // Carga los préstamos enviados por el usuario
    fun cargarPrestados() {
        viewModelScope.launch {
            try {
                _prestados.value = repository.getPrestados()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando préstamos: ${e.message}")
            }
        }
    }

    // Carga los préstamos recibidos por el usuario
    fun cargarRecibidos() {
        viewModelScope.launch {
            try {
                _recibidos.value = repository.getPrestamosRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando préstamos recibidos: ${e.message}")
            }
        }
    }

    // Carga la lista de usuarios disponibles
    fun cargarUsuarios() {
        viewModelScope.launch {
            try {
                _usuarios.value = repository.getUsuarios()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando usuarios: ${e.message}")
            }
        }
    }

    // Crea un nuevo préstamo
    fun crearPrestamo(request: PrestamoRequest) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.crearPrestamo(request)
                _state.value = PrestamoState.Success("Préstamo creado correctamente")

                // Refresca ambas listas para sincronizar cambios
                cargarPrestados()
                cargarRecibidos()

            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al crear el préstamo")
            }
        }
    }

    // Marca un préstamo como devuelto
    fun devolverPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.devolverPrestamo(id)
                _state.value = PrestamoState.Success("Devolución registrada correctamente")

                // Refresca listas tras devolución
                cargarPrestados()
                cargarRecibidos()

            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al registrar la devolución")
            }
        }
    }

    // Elimina un préstamo
    fun eliminarPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.deletePrestamo(id)
                _state.value = PrestamoState.Success("Préstamo eliminado correctamente")

                // Refresca listas tras eliminación
                cargarPrestados()
                cargarRecibidos()

            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al eliminar el préstamo")
            }
        }
    }

    // Reinicia el estado de la UI
    fun resetState() {
        _state.value = PrestamoState.Idle
    }
}