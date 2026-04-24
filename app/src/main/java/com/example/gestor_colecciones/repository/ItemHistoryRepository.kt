package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemHistoryDao
import com.example.gestor_colecciones.entities.ItemHistory

/**
 * Repositorio encargado de gestionar el historial de cambios de items
 */
class ItemHistoryRepository(private val dao: ItemHistoryDao) {

    /**
     * Inserta un registro de historial en la base de datos
     */
    suspend fun insert(history: ItemHistory) = dao.insert(history)

    /**
     * Obtiene el historial de un item específico
     */
    fun getHistoryForItem(itemId: Int) = dao.getHistoryForItem(itemId)
}