package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ExportRepository

// Factory encargada de crear instancias de StatsViewModel
class StatsViewModelFactory(
    private val exportRepository: ExportRepository
) : ViewModelProvider.Factory {

    // Método genérico de creación de ViewModels
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica si el ViewModel solicitado es StatsViewModel
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {

            // Crea el ViewModel inyectando el repositorio
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(exportRepository) as T
        }

        // Si no coincide el tipo, lanza excepción
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}