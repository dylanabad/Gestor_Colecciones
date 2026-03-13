package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItemViewModel(private val repository: ItemRepository) : ViewModel() {

    val items: StateFlow<List<Item>> =
        repository.allItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Función de insert que devuelve el ID del item insertado mediante callback
    fun insert(item: Item, onInserted: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.insert(item).toInt() // insert devuelve Long, convertimos a Int
            onInserted(id) // llamamos al callback con el ID
        }
    }

    fun update(item: Item) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun delete(item: Item) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun getItemsByCollection(collectionId: Int) =
        repository.getItemsByCollection(collectionId)

    fun getItemsByCategoria(categoriaId: Int) =
        repository.getItemsByCategoria(categoriaId)

    fun getItemsByCollectionFlow(collectionId: Int) = repository.getItemsByCollectionFlow(collectionId)

    fun searchItems(search: String) =
        repository.searchItemsByTitle(search)

    suspend fun getTotalItems() =
        repository.getTotalItems()

    suspend fun getTotalValor() =
        repository.getTotalValor()

    // --- NUEVO ---
    suspend fun getItemById(id: Int): Item? =
        repository.getItemById(id)
}

