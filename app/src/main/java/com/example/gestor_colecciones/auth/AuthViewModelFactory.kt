package com.example.gestor_colecciones.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Fabrica de [AuthViewModel] para poder suministrarle el repositorio desde la UI. */
class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {

    /** Crea la instancia solicitada cuando el tipo coincide con [AuthViewModel]. */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
