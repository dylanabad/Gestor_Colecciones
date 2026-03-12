package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.repository.CategoriaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriaViewModel(private val repository: CategoriaRepository) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> =
        repository.allCategorias.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(categoria: Categoria) {
        viewModelScope.launch {
            repository.insert(categoria)
        }
    }

    fun update(categoria: Categoria) {
        viewModelScope.launch {
            repository.update(categoria)
        }
    }

    fun delete(categoria: Categoria) {
        viewModelScope.launch {
            repository.delete(categoria)
        }
    }
}