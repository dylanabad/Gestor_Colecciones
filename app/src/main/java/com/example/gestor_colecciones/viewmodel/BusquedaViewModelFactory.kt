package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.BusquedaRepository

class BusquedaViewModelFactory(
    private val repository: BusquedaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusquedaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusquedaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}