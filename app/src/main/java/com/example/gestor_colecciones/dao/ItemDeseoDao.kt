package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.ItemDeseo
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDeseoDao {

    @Insert
    suspend fun insert(itemDeseo: ItemDeseo): Long

    @Update
    suspend fun update(itemDeseo: ItemDeseo)

    @Delete
    suspend fun delete(itemDeseo: ItemDeseo)

    @Query("SELECT * FROM ItemDeseo WHERE conseguido = 0 ORDER BY prioridad ASC, fechaCreacion DESC")
    fun getPendientes(): Flow<List<ItemDeseo>>

    @Query("SELECT * FROM ItemDeseo WHERE conseguido = 1 ORDER BY fechaConseguido DESC")
    fun getConseguidos(): Flow<List<ItemDeseo>>

    @Query("SELECT * FROM ItemDeseo ORDER BY conseguido ASC, prioridad ASC, fechaCreacion DESC")
    fun getAll(): Flow<List<ItemDeseo>>

    @Query("SELECT COUNT(*) FROM ItemDeseo WHERE conseguido = 0")
    fun countPendientes(): Flow<Int>
}