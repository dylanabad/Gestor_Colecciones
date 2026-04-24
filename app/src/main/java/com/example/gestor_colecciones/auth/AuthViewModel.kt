package com.example.gestor_colecciones.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Representa el estado observable del flujo de autenticacion. */
sealed class AuthState {
    /** Estado inicial, sin operaciones pendientes ni resultados que mostrar. */
    object Idle : AuthState()

    /** Estado transitorio mientras se ejecuta una peticion de autenticacion. */
    object Loading : AuthState()

    /** Estado emitido cuando el backend devuelve una autenticacion valida. */
    data class Success(val response: AuthResponse) : AuthState()

    /** Estado emitido cuando la operacion falla y la UI debe informar al usuario. */
    data class Error(val message: String) : AuthState()
}

/**
 * Orquesta login y registro desde la interfaz, exponiendo un unico flujo de
 * estado para mantener la pantalla desacoplada del repositorio.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    /** Estado interno mutable que solo modifica el ViewModel. */
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)

    /** Estado publico consumido por la UI. */
    val state: StateFlow<AuthState> = _state

    /** Inicia el login clasico por email y contrasena. */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.login(email, password)
                _state.value = AuthState.Success(response)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al iniciar sesion")
            }
        }
    }

    /** Inicia el login estricto validando usuario, email y contrasena. */
    fun loginStrict(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.loginStrict(username, email, password)
                _state.value = AuthState.Success(response)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al iniciar sesion")
            }
        }
    }

    /** Inicia el registro y refleja el resultado en el estado observable. */
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.register(username, email, password)
                _state.value = AuthState.Success(response)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al registrar")
            }
        }
    }
}
