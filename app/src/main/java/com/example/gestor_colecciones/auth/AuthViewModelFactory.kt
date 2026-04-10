package com.example.gestor_colecciones.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Factory encargada de crear instancias de AuthViewModel con dependencias inyectadas
class AuthViewModelFactory(
    private val repository: AuthRepository // Repositorio necesario para el ViewModel
) : ViewModelProvider.Factory {

    // Método encargado de crear el ViewModel solicitado
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Comprueba si el ViewModel solicitado es AuthViewModel
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }

        // Si se solicita otro ViewModel, lanza excepción
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}