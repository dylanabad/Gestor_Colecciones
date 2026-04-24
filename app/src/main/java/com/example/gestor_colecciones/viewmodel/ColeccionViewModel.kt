package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.repository.ColeccionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Expone a la UI el listado de colecciones y las operaciones de mantenimiento
 * sobre ellas.
 */
class ColeccionViewModel(
    private val repository: ColeccionRepository
) : ViewModel() {

    /** Flujo de colecciones activo mientras exista un observador en pantalla. */
    val colecciones: StateFlow<List<Coleccion>> =
        repository.allColecciones.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /** Inserta una nueva coleccion. */
    fun insert(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.insert(coleccion)
        }
    }

    /** Actualiza una coleccion existente. */
    fun update(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.update(coleccion)
        }
    }

    /** Elimina una coleccion. */
    fun delete(coleccion: Coleccion) {
        viewModelScope.launch {
            repository.delete(coleccion)
        }
    }

    /** Recupera una coleccion puntual por id para formularios o pantallas de detalle. */
    fun getColeccionById(id: Int, onResult: (Coleccion?) -> Unit) {
        viewModelScope.launch {
            val coleccion = repository.getById(id)
            onResult(coleccion)
        }
    }
}
