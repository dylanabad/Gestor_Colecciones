package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.repository.ColeccionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ColeccionViewModel(private val repository: ColeccionRepository) : ViewModel() {

    val colecciones: StateFlow<List<Coleccion>> =
        repository.allColecciones.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.insert(coleccion)
        }
    }

    fun update(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.update(coleccion)
        }
    }

    fun delete(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.delete(coleccion)
        }
    }
}