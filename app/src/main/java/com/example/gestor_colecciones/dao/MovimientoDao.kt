package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Movimiento
import kotlinx.coroutines.flow.Flow

// DAO encargado de gestionar los movimientos (historial de acciones sobre items)
@Dao
interface MovimientoDao {

    // Inserta un nuevo movimiento
    @Insert
    suspend fun insert(movimiento: Movimiento): Long

    // Actualiza un movimiento existente
    @Update
    suspend fun update(movimiento: Movimiento)

    // Elimina un movimiento de la base de datos
    @Delete
    suspend fun delete(movimiento: Movimiento)

    // Obtiene todos los movimientos ordenados por fecha descendente
    @Query("SELECT * FROM Movimiento ORDER BY fechaHora DESC")
    fun getAllMovimientos(): Flow<List<Movimiento>>

    // Obtiene los movimientos asociados a un item específico
    @Query("SELECT * FROM Movimiento WHERE itemId = :itemId ORDER BY fechaHora DESC")
    fun getMovimientosByItem(itemId: Int): Flow<List<Movimiento>>

    // Obtiene los movimientos asociados a una persona específica
    @Query("SELECT * FROM Movimiento WHERE personaId = :personaId ORDER BY fechaHora DESC")
    fun getMovimientosByPersona(personaId: Int): Flow<List<Movimiento>>

    // ─────────────────────────────────────────────────────────────
    // PRÉSTAMOS
    // ─────────────────────────────────────────────────────────────

    // Obtiene el préstamo activo de un item (si existe)
    @Query("""
        SELECT * FROM Movimiento 
        WHERE tipo = 'PRESTAMO' 
        AND itemId = :itemId 
        AND estado = 'ACTIVO'
        LIMIT 1
    """)
    suspend fun getPrestamoActivoByItem(itemId: Int): Movimiento?

    // Obtiene todos los préstamos realizados
    @Query("""
        SELECT m.* FROM Movimiento m
        INNER JOIN Item i ON m.itemId = i.id
        WHERE m.tipo = 'PRESTAMO'
        ORDER BY m.fechaHora DESC
    """)
    fun getPrestamosRealizados(): Flow<List<Movimiento>>

    // Obtiene los préstamos recibidos por un usuario
    @Query("""
        SELECT m.* FROM Movimiento m
        INNER JOIN Persona p ON m.personaId = p.id
        WHERE m.tipo = 'PRESTAMO' 
        AND p.usuarioRefId = :usuarioRefId
        ORDER BY m.fechaHora DESC
    """)
    fun getPrestamosRecibidos(usuarioRefId: Int): Flow<List<Movimiento>>
}