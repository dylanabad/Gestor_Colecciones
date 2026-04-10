package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ColeccionRepository

// Factory encargada de crear instancias de ColeccionViewModel con dependencias
class ColeccionViewModelFactory(
    private val repo: ColeccionRepository
) : ViewModelProvider.Factory {

    // Crea el ViewModel solicitado por el sistema
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Comprueba si el ViewModel solicitado es el correcto
        if (modelClass.isAssignableFrom(ColeccionViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return ColeccionViewModel(repo) as T
        }

        // Si no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}