package com.example.gestor_colecciones.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.repository.TagRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TagViewModel(private val repository: TagRepository) : ViewModel() {

    val tags: StateFlow<List<Tag>> =
        repository.allTags.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(tag: Tag) {
        viewModelScope.launch {
            repository.insert(tag)
        }
    }

    fun searchTags(search: String) =
        repository.searchTags(search)
}