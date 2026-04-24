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

/** Estado transversal del modulo de prestamos. */
sealed class PrestamoState {
    object Idle : PrestamoState()
    object Loading : PrestamoState()
    data class Success(val message: String) : PrestamoState()
    data class Error(val message: String) : PrestamoState()
}

/**
 * Coordina la carga y las operaciones de prestamos desde la UI.
 *
 * Mantiene flujos separados para enviados, recibidos y usuarios disponibles,
 * ademas de un estado general para acciones de crear, devolver o eliminar.
 */
class PrestamoViewModel(
    private val repository: PrestamoRepository
) : ViewModel() {

    /** Prestamos emitidos por el usuario autenticado. */
    private val _prestados = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val prestados: StateFlow<List<PrestamoDto>> = _prestados

    /** Prestamos recibidos por el usuario autenticado. */
    private val _recibidos = MutableStateFlow<List<PrestamoDto>>(emptyList())
    val recibidos: StateFlow<List<PrestamoDto>> = _recibidos

    /** Usuarios disponibles como destinatarios de un prestamo. */
    private val _usuarios = MutableStateFlow<List<UsuarioDto>>(emptyList())
    val usuarios: StateFlow<List<UsuarioDto>> = _usuarios

    /** Estado general de las operaciones accionadas por la UI. */
    private val _state = MutableStateFlow<PrestamoState>(PrestamoState.Idle)
    val state: StateFlow<PrestamoState> = _state

    /** Carga la bandeja de prestamos enviados. */
    fun cargarPrestados() {
        viewModelScope.launch {
            try {
                _prestados.value = repository.getPrestados()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando prestamos: ${e.message}")
            }
        }
    }

    /** Carga la bandeja de prestamos recibidos. */
    fun cargarRecibidos() {
        viewModelScope.launch {
            try {
                _recibidos.value = repository.getPrestamosRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando prestamos recibidos: ${e.message}")
            }
        }
    }

    /** Recupera la lista de usuarios disponibles para prestar. */
    fun cargarUsuarios() {
        viewModelScope.launch {
            try {
                _usuarios.value = repository.getUsuarios()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error("Error cargando usuarios: ${e.message}")
            }
        }
    }

    /** Crea un nuevo prestamo y refresca ambas bandejas al completarse. */
    fun crearPrestamo(request: PrestamoRequest) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.crearPrestamo(request)
                _state.value = PrestamoState.Success("Prestamo creado correctamente")
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al crear el prestamo")
            }
        }
    }

    /** Marca un prestamo como devuelto y recarga los listados visibles. */
    fun devolverPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.devolverPrestamo(id)
                _state.value = PrestamoState.Success("Devolucion registrada correctamente")
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al registrar la devolucion")
            }
        }
    }

    /** Elimina un prestamo y actualiza las bandejas del modulo. */
    fun eliminarPrestamo(id: Long) {
        viewModelScope.launch {
            _state.value = PrestamoState.Loading
            try {
                repository.deletePrestamo(id)
                _state.value = PrestamoState.Success("Prestamo eliminado correctamente")
                cargarPrestados()
                cargarRecibidos()
            } catch (e: Exception) {
                _state.value = PrestamoState.Error(e.message ?: "Error al eliminar el prestamo")
            }
        }
    }

    /** Devuelve el estado general a reposo tras consumir un mensaje o resultado. */
    fun resetState() {
        _state.value = PrestamoState.Idle
    }
}
