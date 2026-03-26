package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.PrestamoRepository

class PrestamoViewModelFactory(
    private val repository: PrestamoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrestamoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrestamoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}