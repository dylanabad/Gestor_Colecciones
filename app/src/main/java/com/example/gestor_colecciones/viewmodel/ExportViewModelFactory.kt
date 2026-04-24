package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ExportRepository

/**
 * Factory encargada de crear instancias de ExportViewModel con sus dependencias
 */
class ExportViewModelFactory(
    private val exportRepository: ExportRepository
) : ViewModelProvider.Factory {

    /**
     * Crea el ViewModel solicitado por el sistema
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Comprueba si el ViewModel solicitado es ExportViewModel
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return ExportViewModel(exportRepository) as T
        }

        // Si no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}