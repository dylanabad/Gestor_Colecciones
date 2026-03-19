package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.ItemHistory
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.CategoriaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val categoriaRepository: CategoriaRepository? = null, // opcional para crear categorías
    private val historyRepository: ItemHistoryRepository? = null
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
            historyRepository?.insert(
                ItemHistory(
                    itemId = id,
                    tipo = "CREATED",
                    fecha = Date(),
                    descripcion = "Item creado"
                )
            )
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
            val old = historyRepository?.let { itemRepository.getItemById(item.id) }
            itemRepository.update(item)

            if (historyRepository != null) {
                val now = Date()
                if (old != null) {
                    val estadoChanged = old.estado != item.estado
                    val otherChanged = old.copy(estado = item.estado) != item

                    if (estadoChanged) {
                        historyRepository.insert(
                            ItemHistory(
                                itemId = item.id,
                                tipo = "STATUS_CHANGED",
                                fecha = now,
                                descripcion = "Estado: ${old.estado} \u2192 ${item.estado}"
                            )
                        )
                    }

                    if (otherChanged) {
                        val fields = buildList {
                            if (old.titulo != item.titulo) add("Título")
                            if (old.valor != item.valor) add("Valor")
                            if (old.descripcion != item.descripcion) add("Descripción")
                            if (old.categoriaId != item.categoriaId) add("Categoría")
                            if (old.imagenPath != item.imagenPath) add("Imagen")
                            if (old.calificacion != item.calificacion) add("Calificación")
                        }
                        historyRepository.insert(
                            ItemHistory(
                                itemId = item.id,
                                tipo = "EDITED",
                                fecha = now,
                                descripcion = if (fields.isEmpty()) "Item actualizado" else "Editado: ${fields.joinToString()}"
                            )
                        )
                    }
                } else {
                    historyRepository.insert(
                        ItemHistory(
                            itemId = item.id,
                            tipo = "EDITED",
                            fecha = now,
                            descripcion = "Item actualizado"
                        )
                    )
                }
            }
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
