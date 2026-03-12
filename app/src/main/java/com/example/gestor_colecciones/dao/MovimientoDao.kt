package com.example.gestor_colecciones.dao


import androidx.room.*
import com.example.gestor_colecciones.entities.Movimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insert(movimiento: Movimiento): Long

    @Update
    suspend fun update(movimiento: Movimiento)

    @Delete
    suspend fun delete(movimiento: Movimiento)

    @Query("SELECT * FROM Movimiento ORDER BY fechaHora DESC")
    fun getAllMovimientos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM Movimiento WHERE itemId = :itemId ORDER BY fechaHora DESC")
    fun getMovimientosByItem(itemId: Int): Flow<List<Movimiento>>

    @Query("SELECT * FROM Movimiento WHERE personaId = :personaId ORDER BY fechaHora DESC")
    fun getMovimientosByPersona(personaId: Int): Flow<List<Movimiento>>
}