package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.util.Date

class ItemRepository(
    private val itemDao: ItemDao,
    private val api: ApiService
) {

    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insert(item: Item): Long {
        try {
            val saved = api.saveItem(
                item.collectionId.toLong(),
                item.categoriaId.takeIf { it > 0 }?.toLong(),
                item.toDto()
            )
            return itemDao.insert(saved.toEntity(item.collectionId))
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    suspend fun update(item: Item) {
        try {
            val saved = api.saveItem(
                item.collectionId.toLong(),
                item.categoriaId.takeIf { it > 0 }?.toLong(),
                item.toDto()
            )
            itemDao.insert(saved.toEntity(item.collectionId))
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    suspend fun delete(item: Item) {
        try {
            api.deleteItem(item.id.toLong())
            itemDao.update(item.copy(eliminado = true, fechaEliminacion = Date()))
        } catch (e: HttpException) {
            throw RuntimeException(extractError(e))
        }
    }

    fun getItemsByCollection(collectionId: Int): Flow<List<Item>> =
        itemDao.getItemsByCollection(collectionId)

    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>> =
        itemDao.getItemsByCategoria(categoriaId)

    fun searchItemsByTitle(search: String): Flow<List<Item>> =
        itemDao.searchItemsByTitle(search)

    suspend fun getTotalItems(): Int = itemDao.getTotalItems()
    suspend fun getTotalValor(): Double = itemDao.getTotalValor() ?: 0.0

    fun getItemsByCollectionFlow(collectionId: Int): Flow<List<Item>> {
        return itemDao.getItemsByCollection(collectionId)
    }

    suspend fun getItemById(id: Int): Item? = itemDao.getItemById(id)

    suspend fun getItemsByCollectionOnce(collectionId: Int): List<Item> =
        itemDao.getItemsByCollectionOnce(collectionId)

    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }
}