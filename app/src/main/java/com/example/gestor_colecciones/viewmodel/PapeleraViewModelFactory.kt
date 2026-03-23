package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.PapeleraRepository

class PapeleraViewModelFactory(
    private val repository: PapeleraRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PapeleraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PapeleraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}