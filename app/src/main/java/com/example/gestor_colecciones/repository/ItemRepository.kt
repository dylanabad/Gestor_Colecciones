package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {

    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insert(item: Item) = itemDao.insert(item)
    suspend fun update(item: Item) = itemDao.update(item)
    suspend fun delete(item: Item) = itemDao.delete(item)

    fun getItemsByCollection(collectionId: Int): Flow<List<Item>> =
        itemDao.getItemsByCollection(collectionId)

    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>> =
        itemDao.getItemsByCategoria(categoriaId)

    fun searchItemsByTitle(search: String): Flow<List<Item>> =
        itemDao.searchItemsByTitle(search)

    suspend fun getTotalItems(): Int = itemDao.getTotalItems()
    suspend fun getTotalValor(): Double = itemDao.getTotalValor() ?: 0.0  // ← cambiado

    fun getItemsByCollectionFlow(collectionId: Int): Flow<List<Item>> {
        return itemDao.getItemsByCollection(collectionId)
    }

    suspend fun getItemById(id: Int): Item? = itemDao.getItemById(id)

    // Exportación — consulta one-shot sin Flow
    suspend fun getItemsByCollectionOnce(collectionId: Int): List<Item> =
        itemDao.getItemsByCollectionOnce(collectionId)
}