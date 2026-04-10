package com.example.gestor_colecciones.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estados posibles del proceso de autenticación
sealed class AuthState {

    // Estado inicial sin acción
    object Idle : AuthState()

    // Estado mientras se está realizando la petición
    object Loading : AuthState()

    // Estado cuando la autenticación es correcta
    data class Success(val response: AuthResponse) : AuthState()

    // Estado cuando ocurre un error
    data class Error(val message: String) : AuthState()
}

// ViewModel que gestiona el flujo de autenticación
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // Estado interno mutable
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)

    // Estado expuesto de forma inmutable a la UI
    val state: StateFlow<AuthState> = _state

    // Inicia el proceso de login
    fun login(email: String, password: String) {
        viewModelScope.launch {

            // Cambia a estado de carga
            _state.value = AuthState.Loading

            try {
                // Llamada al repositorio
                val response = repository.login(email, password)

                // Estado de éxito con respuesta
                _state.value = AuthState.Success(response)

            } catch (e: Exception) {

                // Estado de error con mensaje
                _state.value =
                    AuthState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    // Inicia el proceso de registro
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {

            // Cambia a estado de carga
            _state.value = AuthState.Loading

            try {
                // Llamada al repositorio
                val response = repository.register(username, email, password)

                // Estado de éxito con respuesta
                _state.value = AuthState.Success(response)

            } catch (e: Exception) {

                // Estado de error con mensaje
                _state.value =
                    AuthState.Error(e.message ?: "Error al registrar")
            }
        }
    }
}