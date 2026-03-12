package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.repository.ItemTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemTagViewModel(private val repository: ItemTagRepository) : ViewModel() {

    fun insert(itemTag: ItemTag) {
        viewModelScope.launch {
            repository.insert(itemTag)
        }
    }

    fun delete(itemTag: ItemTag) {
        viewModelScope.launch {
            repository.delete(itemTag)
        }
    }

    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>> =
        repository.getTagsForItem(itemId)

    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>> =
        repository.getItemsForTag(tagId)
}