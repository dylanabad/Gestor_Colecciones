package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.PapeleraRepository

// Factory encargada de crear instancias de PapeleraViewModel
class PapeleraViewModelFactory(
    private val repository: PapeleraRepository
) : ViewModelProvider.Factory {

    // Método genérico para crear ViewModels
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica que el ViewModel solicitado sea el correcto
        if (modelClass.isAssignableFrom(PapeleraViewModel::class.java)) {

            // Creación del ViewModel con su repositorio inyectado
            @Suppress("UNCHECKED_CAST")
            return PapeleraViewModel(repository) as T
        }

        // Si no coincide el tipo, lanza excepción
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}