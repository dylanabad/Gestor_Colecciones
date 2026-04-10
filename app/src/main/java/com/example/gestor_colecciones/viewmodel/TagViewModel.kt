package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel encargado de gestionar las etiquetas (tags)
class TagViewModel(
    private val repository: TagRepository
) : ViewModel() {

    // Lista reactiva de tags disponibles
    val tags: StateFlow<List<Tag>> =
        repository.allTags.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Inserta un nuevo tag
    fun insert(tag: Tag) {
        viewModelScope.launch {
            repository.insert(tag)
        }
    }

    // Busca tags según texto introducido
    fun searchTags(search: String) =
        repository.searchTags(search)
}