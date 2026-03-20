package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.repository.LogroRepository

class LogroViewModelFactory(
    private val logroRepository: LogroRepository,
    private val logroManager: LogroManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogroViewModel(logroRepository, logroManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}