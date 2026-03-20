package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ItemDeseoRepository

class DeseoViewModelFactory(
    private val repository: ItemDeseoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeseoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeseoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}