package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.repository.CategoriaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar categorías en la UI
 */
class CategoriaViewModel(
    private val repository: CategoriaRepository
) : ViewModel() {

    /**
     * Lista reactiva de categorías expuesta a la UI
     */
    val categorias: StateFlow<List<Categoria>> =
        repository.allCategorias.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Inserta una nueva categoría
     */
    fun insert(categoria: Categoria) {
        viewModelScope.launch {
            repository.insert(categoria)
        }
    }

    /**
     * Actualiza una categoría existente
     */
    fun update(categoria: Categoria) {
        viewModelScope.launch {
            repository.update(categoria)
        }
    }

    /**
     * Elimina una categoría
     */
    fun delete(categoria: Categoria) {
        viewModelScope.launch {
            repository.delete(categoria)
        }
    }
}