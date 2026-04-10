package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.repository.ColeccionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel encargado de gestionar colecciones en la UI
class ColeccionViewModel(
    private val repository: ColeccionRepository
) : ViewModel() {

    // Lista reactiva de colecciones expuesta a la UI
    val colecciones: StateFlow<List<Coleccion>> =
        repository.allColecciones.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Inserta una nueva colección
    fun insert(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.insert(coleccion)
        }
    }

    // Actualiza una colección existente
    fun update(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.update(coleccion)
        }
    }

    // Elimina una colección
    fun delete(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.delete(coleccion)
        }
    }

    // Obtiene una colección por su ID y devuelve el resultado mediante callback
    fun getColeccionById(id: Int, onResult: (Coleccion?) -> Unit) {
        viewModelScope.launch {
            val coleccion = repository.getById(id)
            onResult(coleccion)
        }
    }
}