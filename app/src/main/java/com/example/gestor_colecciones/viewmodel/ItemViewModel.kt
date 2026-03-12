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

    fun insert(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
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

    fun searchItems(search: String) =
        repository.searchItemsByTitle(search)

    suspend fun getTotalItems() =
        repository.getTotalItems()

    suspend fun getTotalValor() =
        repository.getTotalValor()
}