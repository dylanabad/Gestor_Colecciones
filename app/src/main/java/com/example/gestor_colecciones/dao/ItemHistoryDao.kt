package com.example.gestor_colecciones.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gestor_colecciones.entities.ItemHistory
import kotlinx.coroutines.flow.Flow

// DAO encargado del historial de cambios de los items
@Dao
interface ItemHistoryDao {

    // Inserta un registro en el historial de un item
    @Insert
    suspend fun insert(history: ItemHistory): Long

    // Obtiene el historial de un item ordenado por fecha (desc) e ID (desc)
    @Query("SELECT * FROM item_history WHERE itemId = :itemId ORDER BY fecha DESC, id DESC")
    fun getHistoryForItem(itemId: Int): Flow<List<ItemHistory>>
}