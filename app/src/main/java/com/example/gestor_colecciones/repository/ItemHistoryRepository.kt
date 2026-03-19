package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemHistoryDao
import com.example.gestor_colecciones.entities.ItemHistory

class ItemHistoryRepository(private val dao: ItemHistoryDao) {
    suspend fun insert(history: ItemHistory) = dao.insert(history)
    fun getHistoryForItem(itemId: Int) = dao.getHistoryForItem(itemId)
}

