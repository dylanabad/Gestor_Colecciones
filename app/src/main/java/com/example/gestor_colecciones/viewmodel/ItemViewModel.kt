package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.CategoriaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val categoriaRepository: CategoriaRepository? = null // opcional para crear categorías
) : ViewModel() {

    val items: StateFlow<List<Item>> =
        itemRepository.allItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Insertar item con callback que devuelve el ID
    fun insert(item: Item, onInserted: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val id = itemRepository.insert(item).toInt()
            onInserted?.invoke(id)
        }
    }

    fun update(item: Item) {
        update(item, null)
    }

    fun delete(item: Item) {
        delete(item, null)
    }

    fun update(item: Item, onUpdated: (() -> Unit)? = null) {
        viewModelScope.launch {
            itemRepository.update(item)
            onUpdated?.invoke()
        }
    }

    fun delete(item: Item, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            itemRepository.delete(item)
            onDeleted?.invoke()
        }
    }

    fun getItemsByCollection(collectionId: Int) = itemRepository.getItemsByCollection(collectionId)

    fun getItemsByCategoria(categoriaId: Int) = itemRepository.getItemsByCategoria(categoriaId)

    fun getItemsByCollectionFlow(collectionId: Int) = itemRepository.getItemsByCollectionFlow(collectionId)

    fun searchItems(search: String) = itemRepository.searchItemsByTitle(search)

    suspend fun getTotalItems() = itemRepository.getTotalItems()

    suspend fun getTotalValor() = itemRepository.getTotalValor()

    suspend fun getItemById(id: Int) = itemRepository.getItemById(id)

    // --- NUEVO: Funciones para categorías ---
    suspend fun insertCategoria(nombre: String): Categoria? {
        return if (categoriaRepository != null && nombre.isNotBlank()) {
            val categoria = Categoria(nombre = nombre)
            categoriaRepository.insert(categoria)
            categoria
        } else null
    }

    suspend fun getAllCategorias(): List<Categoria> {
        return categoriaRepository?.allCategoriasOnce() ?: emptyList()
    }
}
