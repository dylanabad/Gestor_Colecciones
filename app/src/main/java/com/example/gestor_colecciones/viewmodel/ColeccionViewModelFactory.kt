package com.example.gestor_colecciones.viewmodel
import com.example.gestor_colecciones.repository.ColeccionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
class ColeccionViewModelFactory(private val repo: ColeccionRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ColeccionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ColeccionViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}