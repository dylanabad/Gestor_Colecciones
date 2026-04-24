package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.ItemDeseo
import kotlinx.coroutines.flow.Flow

/**
 * Contrato Room para la lista de deseos y su gestion de borrado logico.
 */
@Dao
interface ItemDeseoDao {

    // Inserta un item de deseo; reemplaza si hay conflicto
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(itemDeseo: ItemDeseo): Long

    // Inserta una lista de items de deseo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemDeseo>)

    // Actualiza un item de deseo existente
    @Update
    suspend fun update(itemDeseo: ItemDeseo)

    // Elimina un item de deseo de la base de datos
    @Delete
    suspend fun delete(itemDeseo: ItemDeseo)

    // ─────────────────────────────────────────────────────────────
    // ITEMS PENDIENTES
    // ─────────────────────────────────────────────────────────────

    // Obtiene los deseos no conseguidos ordenados por prioridad y fecha de creación
    @Query("""
        SELECT * FROM ItemDeseo 
        WHERE conseguido = 0 AND eliminado = 0
        ORDER BY prioridad ASC, fechaCreacion DESC
    """)
    fun getPendientes(): Flow<List<ItemDeseo>>

    // ─────────────────────────────────────────────────────────────
    // ITEMS CONSEGUIDOS
    // ─────────────────────────────────────────────────────────────

    // Obtiene los deseos ya conseguidos ordenados por fecha de consecución
    @Query("""
        SELECT * FROM ItemDeseo 
        WHERE conseguido = 1 AND eliminado = 0
        ORDER BY fechaConseguido DESC
    """)
    fun getConseguidos(): Flow<List<ItemDeseo>>

    // ─────────────────────────────────────────────────────────────
    // LISTADO COMPLETO
    // ─────────────────────────────────────────────────────────────

    // Obtiene todos los deseos ordenados por estado, prioridad y fecha
    @Query("""
        SELECT * FROM ItemDeseo 
        WHERE eliminado = 0
        ORDER BY conseguido ASC, prioridad ASC, fechaCreacion DESC
    """)
    fun getAll(): Flow<List<ItemDeseo>>

    // Cuenta los deseos pendientes
    @Query("SELECT COUNT(*) FROM ItemDeseo WHERE conseguido = 0 AND eliminado = 0")
    fun countPendientes(): Flow<Int>

    // ── Papelera ───────────────────────────────────────────────────────
    @Query("SELECT * FROM ItemDeseo WHERE eliminado = 1 ORDER BY fechaEliminacion DESC")
    fun getDeseosEliminados(): Flow<List<ItemDeseo>>

    // VacÃ­a la papelera de deseos (eliminaciÃ³n fÃ­sica local)
    @Query("DELETE FROM ItemDeseo WHERE eliminado = 1")
    suspend fun deleteAllEliminados()

    @Query("DELETE FROM ItemDeseo WHERE eliminado = 1 AND fechaEliminacion < :fecha")
    suspend fun limpiarDeseosAntiguos(fecha: Long)
}
