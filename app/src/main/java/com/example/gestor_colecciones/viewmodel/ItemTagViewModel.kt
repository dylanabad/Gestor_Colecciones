package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.repository.ItemTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ViewModel encargado de gestionar la relación muchos-a-muchos entre items y tags
class ItemTagViewModel(
    private val repository: ItemTagRepository
) : ViewModel() {

    // Inserta una relación item-tag en la base de datos
    fun insert(itemTag: ItemTag) {
        viewModelScope.launch {
            repository.insert(itemTag)
        }
    }

    // Elimina una relación item-tag de la base de datos
    fun delete(itemTag: ItemTag) {
        viewModelScope.launch {
            repository.delete(itemTag)
        }
    }

    // Obtiene los tags asociados a un item
    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>> =
        repository.getTagsForItem(itemId)

    // Obtiene los items asociados a un tag
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>> =
        repository.getItemsForTag(tagId)
}