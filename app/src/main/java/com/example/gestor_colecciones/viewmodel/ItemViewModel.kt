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

/**
 * ViewModel encargado de la lógica de Items y su historial
 */
class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val categoriaRepository: CategoriaRepository? = null, // opcional: gestión de categorías
    private val historyRepository: ItemHistoryRepository? = null  // opcional: historial de cambios
) : ViewModel() {

    /**
     * Lista reactiva de items
     */
    val items: StateFlow<List<Item>> =
        itemRepository.allItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Inserta un item y registra su creación en el historial
     */
    fun insert(
        item: Item,
        onInserted: ((Int) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {

                /**
                 * Inserta item en repositorio y obtiene su ID
                 */
                val id = itemRepository.insert(item).toInt()

                // Registra historial de creación si existe repositorio
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

    /**
     * Sobrecarga simple de update sin callbacks
     */
    fun update(item: Item) {
        update(item, null, null)
    }

    /**
     * Elimina un item sin callbacks
     */
    fun delete(item: Item) {
        delete(item, null, null)
    }

    /**
     * Actualiza un item y registra cambios en el historial
     */
    fun update(
        item: Item,
        onUpdated: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {

                /**
                 * Obtiene estado anterior (solo si hay historial habilitado)
                 */
                val old = historyRepository?.let {
                    itemRepository.getItemById(item.id)
                }

                // Actualiza el item en base de datos
                itemRepository.update(item)

                // Si hay historial disponible, registra cambios
                if (historyRepository != null) {

                    val now = Date()

                    if (old != null) {

                        /**
                         * Detecta cambio de estado
                         */
                        val estadoChanged = old.estado != item.estado

                        /**
                         * Detecta otros cambios generales
                         */
                        val otherChanged = old.copy(estado = item.estado) != item

                        // Historial por cambio de estado
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

                        // Historial por edición de campos
                        if (otherChanged) {

                            val fields = buildList {
                                if (old.titulo != item.titulo) add("Titulo")
                                if (old.valor != item.valor) add("Valor")
                                if (old.descripcion != item.descripcion) add("Descripcion")
                                if (old.categoriaId != item.categoriaId) add("Categoria")
                                if (old.imagenPath != item.imagenPath) add("Imagen")
                                if (old.calificacion != item.calificacion) add("Calificacion")
                                if (old.favorito != item.favorito) add("Favorito")
                            }

                            historyRepository.insert(
                                ItemHistory(
                                    itemId = item.id,
                                    tipo = "EDITED",
                                    fecha = now,
                                    descripcion =
                                        if (fields.isEmpty())
                                            "Item actualizado"
                                        else
                                            "Editado: ${fields.joinToString()}"
                                )
                            )
                        }

                    } else {
                        // Caso sin referencia previa
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

    /**
     * Elimina un item
     */
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

    /**
     * Obtiene items por colección (Flow directo)
     */
    fun getItemsByCollection(collectionId: Int) =
        itemRepository.getItemsByCollection(collectionId)

    /**
     * Obtiene items por categoría
     */
    fun getItemsByCategoria(categoriaId: Int) =
        itemRepository.getItemsByCategoria(categoriaId)

    /**
     * Flow específico por colección
     */
    fun getItemsByCollectionFlow(collectionId: Int) =
        itemRepository.getItemsByCollectionFlow(collectionId)

    /**
     * Búsqueda por título
     */
    fun searchItems(search: String) =
        itemRepository.searchItemsByTitle(search)

    /**
     * Estadísticas
     */
    suspend fun getTotalItems() =
        itemRepository.getTotalItems()

    suspend fun getTotalValor() =
        itemRepository.getTotalValor()

    suspend fun getItemById(id: Int) =
        itemRepository.getItemById(id)

    /**
     * Inserta una nueva categoría si el repositorio existe
     */
    suspend fun insertCategoria(nombre: String): Categoria? {
        return if (categoriaRepository != null && nombre.isNotBlank()) {
            val categoria = Categoria(nombre = nombre)
            categoriaRepository.insert(categoria)
            categoria
        } else null
    }

    /**
     * Obtiene todas las categorías (o lista vacía si no hay repo)
     */
    suspend fun getAllCategorias(): List<Categoria> {
        return categoriaRepository?.allCategoriasOnce() ?: emptyList()
    }
}