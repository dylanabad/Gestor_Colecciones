package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.BusquedaRepository

/**
 * Factory para crear instancias de BusquedaViewModel con dependencias inyectadas
 */
class BusquedaViewModelFactory(
    private val repository: BusquedaRepository // Repositorio necesario para el ViewModel
) : ViewModelProvider.Factory {

    /**
     * Método encargado de crear ViewModels
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica si el ViewModel solicitado es el correcto
        if (modelClass.isAssignableFrom(BusquedaViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return BusquedaViewModel(repository) as T
        }

        // Si se solicita otro ViewModel, lanza excepción
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}