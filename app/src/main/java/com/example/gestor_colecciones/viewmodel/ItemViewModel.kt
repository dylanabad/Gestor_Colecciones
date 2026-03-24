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
    private val categoriaRepository: CategoriaRepository? = null, // opcional para crear categorias
    private val historyRepository: ItemHistoryRepository? = null
) : ViewModel() {

    val items: StateFlow<List<Item>> =
        itemRepository.allItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(
        item: Item,
        onInserted: ((Int) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Error al crear el item")
            }
        }
    }

    fun update(item: Item) {
        update(item, null, null)
    }

    fun delete(item: Item) {
        delete(item, null, null)
    }

    fun update(
        item: Item,
        onUpdated: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
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
                                    descripcion = "Estado: ${old.estado} -> ${item.estado}"
                                )
                            )
                        }

                        if (otherChanged) {
                            val fields = buildList {
                                if (old.titulo != item.titulo) add("Titulo")
                                if (old.valor != item.valor) add("Valor")
                                if (old.descripcion != item.descripcion) add("Descripcion")
                                if (old.categoriaId != item.categoriaId) add("Categoria")
                                if (old.imagenPath != item.imagenPath) add("Imagen")
                                if (old.calificacion != item.calificacion) add("Calificacion")
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
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Error al actualizar el item")
            }
        }
    }

    fun delete(
        item: Item,
        onDeleted: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                itemRepository.delete(item)
                onDeleted?.invoke()
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Error al eliminar el item")
            }
        }
    }

    fun getItemsByCollection(collectionId: Int) = itemRepository.getItemsByCollection(collectionId)

    fun getItemsByCategoria(categoriaId: Int) = itemRepository.getItemsByCategoria(categoriaId)

    fun getItemsByCollectionFlow(collectionId: Int) = itemRepository.getItemsByCollectionFlow(collectionId)

    fun searchItems(search: String) = itemRepository.searchItemsByTitle(search)

    suspend fun getTotalItems() = itemRepository.getTotalItems()

    suspend fun getTotalValor() = itemRepository.getTotalValor()

    suspend fun getItemById(id: Int) = itemRepository.getItemById(id)

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