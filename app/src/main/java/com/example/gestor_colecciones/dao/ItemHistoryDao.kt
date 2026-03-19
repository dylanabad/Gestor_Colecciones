package com.example.gestor_colecciones.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gestor_colecciones.entities.ItemHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemHistoryDao {

    @Insert
    suspend fun insert(history: ItemHistory): Long

    @Query("SELECT * FROM item_history WHERE itemId = :itemId ORDER BY fecha DESC, id DESC")
    fun getHistoryForItem(itemId: Int): Flow<List<ItemHistory>>
}

