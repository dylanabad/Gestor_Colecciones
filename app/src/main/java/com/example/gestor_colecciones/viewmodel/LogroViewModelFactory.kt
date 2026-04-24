package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.repository.LogroRepository

/**
 * Factory encargada de crear instancias de LogroViewModel con sus dependencias
 */
class LogroViewModelFactory(
    private val logroRepository: LogroRepository,
    private val logroManager: LogroManager
) : ViewModelProvider.Factory {

    /**
     * Crea el ViewModel solicitado por el sistema
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica si el ViewModel solicitado es LogroViewModel
        if (modelClass.isAssignableFrom(LogroViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return LogroViewModel(logroRepository, logroManager) as T
        }

        // Si no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}