package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.util.Date

// Repositorio encargado de gestionar los items, combinando base de datos local y API remota
class ItemRepository(
    private val itemDao: ItemDao, // Acceso a la base de datos local
    private val api: ApiService   // Acceso a la API remota
) {

    // Flujo con todos los items almacenados localmente
    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    // Inserta un item en la API y lo guarda en la base de datos local
    suspend fun insert(item: Item): Long {
        try {

            // Llama a la API para guardar el item en el servidor
            val saved = api.saveItem(
                item.collectionId.toLong(), // ID de la colección
                item.categoriaId.takeIf { it > 0 }?.toLong(), // categoría opcional
                item.toDto() // conversión a DTO
            )

            // Guarda en local la versión devuelta por la API
            return itemDao.insert(saved.toEntity(item.collectionId))

        } catch (e: HttpException) {
            // Convierte el error HTTP en excepción legible
            throw RuntimeException(extractError(e))
        }
    }

    // Actualiza un item en API y base de datos local
    suspend fun update(item: Item) {
        try {

            // Envía actualización al servidor
            val saved = api.saveItem(
                item.collectionId.toLong(),
                item.categoriaId.takeIf { it > 0 }?.toLong(),
                item.toDto()
            )

            // Actualiza/insert en base de datos local
            itemDao.insert(saved.toEntity(item.collectionId))

        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    // Elimina un item tanto en la API como en local (soft delete)
    suspend fun delete(item: Item) {
        try {

            // Elimina en el servidor
            api.deleteItem(item.id.toLong())

            // Marca como eliminado en local con fecha
            itemDao.update(
                item.copy(
                    eliminado = true,
                    fechaEliminacion = Date()
                )
            )

        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    // Obtiene items de una colección como Flow reactivo
    fun getItemsByCollection(collectionId: Int): Flow<List<Item>> =
        itemDao.getItemsByCollection(collectionId)

    // Obtiene items por categoría como Flow reactivo
    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>> =
        itemDao.getItemsByCategoria(categoriaId)

    // Búsqueda de items por título
    fun searchItemsByTitle(search: String): Flow<List<Item>> =
        itemDao.searchItemsByTitle(search)

    // Total de items en la base de datos
    suspend fun getTotalItems(): Int =
        itemDao.getTotalItems()

    // Suma total del valor de los items
    suspend fun getTotalValor(): Double =
        itemDao.getTotalValor() ?: 0.0

    // Alias de flujo por colección (redundante pero útil para UI)
    fun getItemsByCollectionFlow(collectionId: Int): Flow<List<Item>> =
        itemDao.getItemsByCollection(collectionId)

    // Obtiene un item por ID
    suspend fun getItemById(id: Int): Item? =
        itemDao.getItemById(id)

    // Obtiene items de una colección en una sola consulta (no Flow)
    suspend fun getItemsByCollectionOnce(collectionId: Int): List<Item> =
        itemDao.getItemsByCollectionOnce(collectionId)

    // Extrae un mensaje de error legible desde una HttpException
    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }
}