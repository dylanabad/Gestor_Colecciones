package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ItemDeseoRepository

/**
 * Factory encargada de crear instancias de DeseoViewModel con sus dependencias
 */
class DeseoViewModelFactory(
    private val repository: ItemDeseoRepository
) : ViewModelProvider.Factory {

    /**
     * Crea el ViewModel solicitado por el sistema
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Comprueba si el ViewModel solicitado es DeseoViewModel
        if (modelClass.isAssignableFrom(DeseoViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return DeseoViewModel(repository) as T
        }

        // Si no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}