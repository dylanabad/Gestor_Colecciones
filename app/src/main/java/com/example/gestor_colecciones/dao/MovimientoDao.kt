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

    // ── Préstamos ─────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM Movimiento 
        WHERE tipo = 'PRESTAMO' AND itemId = :itemId AND estado = 'ACTIVO'
        LIMIT 1
    """)
    suspend fun getPrestamoActivoByItem(itemId: Int): Movimiento?

    @Query("""
        SELECT m.* FROM Movimiento m
        INNER JOIN Item i ON m.itemId = i.id
        WHERE m.tipo = 'PRESTAMO'
        ORDER BY m.fechaHora DESC
    """)
    fun getPrestamosRealizados(): Flow<List<Movimiento>>

    @Query("""
        SELECT m.* FROM Movimiento m
        INNER JOIN Persona p ON m.personaId = p.id
        WHERE m.tipo = 'PRESTAMO' AND p.usuarioRefId = :usuarioRefId
        ORDER BY m.fechaHora DESC
    """)
    fun getPrestamosRecibidos(usuarioRefId: Int): Flow<List<Movimiento>>
}