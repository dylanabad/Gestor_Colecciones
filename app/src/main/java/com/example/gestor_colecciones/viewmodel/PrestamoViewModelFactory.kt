package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.PrestamoRepository

// Factory para crear instancias de PrestamoViewModel con su repositorio
class PrestamoViewModelFactory(
    private val repository: PrestamoRepository
) : ViewModelProvider.Factory {

    // Método genérico de creación de ViewModels
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica que el ViewModel solicitado sea el correcto
        if (modelClass.isAssignableFrom(PrestamoViewModel::class.java)) {

            // Crea el ViewModel inyectando el repositorio
            @Suppress("UNCHECKED_CAST")
            return PrestamoViewModel(repository) as T
        }

        // Si el tipo no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}