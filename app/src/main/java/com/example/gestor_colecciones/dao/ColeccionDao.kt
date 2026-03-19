package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Coleccion
import kotlinx.coroutines.flow.Flow

@Dao
interface ColeccionDao {

    @Insert
    suspend fun insert(coleccion: Coleccion): Long

    @Update
    suspend fun update(coleccion: Coleccion)

    @Delete
    suspend fun delete(coleccion: Coleccion)

    @Query("SELECT * FROM Coleccion ORDER BY fechaCreacion DESC")
    fun getAllColecciones(): Flow<List<Coleccion>>

    @Query("SELECT * FROM Coleccion WHERE id = :id")
    suspend fun getColeccionById(id: Int): Coleccion?

    @Query("SELECT * FROM Coleccion ORDER BY fechaCreacion DESC")
    suspend fun getAllColeccionesOnce(): List<Coleccion>
}