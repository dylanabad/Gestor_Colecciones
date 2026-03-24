package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Coleccion
import kotlinx.coroutines.flow.Flow

@Dao
interface ColeccionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coleccion: Coleccion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colecciones: List<Coleccion>)

    @Update
    suspend fun update(coleccion: Coleccion)

    @Delete
    suspend fun delete(coleccion: Coleccion)

    // Solo colecciones activas
    @Query("SELECT * FROM Coleccion WHERE eliminado = 0 ORDER BY fechaCreacion DESC")
    fun getAllColecciones(): Flow<List<Coleccion>>

    @Query("SELECT * FROM Coleccion WHERE id = :id")
    suspend fun getColeccionById(id: Int): Coleccion?

    @Query("SELECT * FROM Coleccion WHERE eliminado = 0 ORDER BY fechaCreacion DESC")
    suspend fun getAllColeccionesOnce(): List<Coleccion>

    // ── Papelera ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM Coleccion WHERE eliminado = 1 ORDER BY fechaEliminacion DESC")
    fun getColeccionesEliminadas(): Flow<List<Coleccion>>

    @Query("DELETE FROM Coleccion WHERE eliminado = 1 AND fechaEliminacion < :fecha")
    suspend fun limpiarColeccionesAntiguas(fecha: Long)

    @Query("SELECT * FROM Coleccion WHERE eliminado = 0 AND (nombre LIKE '%' || :search || '%' OR descripcion LIKE '%' || :search || '%') ORDER BY fechaCreacion DESC")
    suspend fun searchColecciones(search: String): List<Coleccion>

}
