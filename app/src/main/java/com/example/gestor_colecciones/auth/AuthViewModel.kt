package com.example.gestor_colecciones.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.network.dto.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.login(email, password)
                _state.value = AuthState.Success(response)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

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
