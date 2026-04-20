package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Coleccion
import kotlinx.coroutines.flow.Flow

// DAO encargado del acceso a datos de la entidad Coleccion
@Dao
interface ColeccionDao {

    // Inserta una colección; si existe conflicto, la reemplaza
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coleccion: Coleccion): Long

    // Inserta una lista de colecciones; reemplaza en caso de conflicto
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colecciones: List<Coleccion>)

    // Actualiza una colección existente
    @Update
    suspend fun update(coleccion: Coleccion)

    // Elimina una colección de la base de datos
    @Delete
    suspend fun delete(coleccion: Coleccion)

    // ─────────────────────────────────────────────────────────────
    // COLECCIONES ACTIVAS
    // ─────────────────────────────────────────────────────────────

    // Obtiene todas las colecciones activas ordenadas por fecha de creación (desc)
    @Query("SELECT * FROM Coleccion WHERE eliminado = 0 ORDER BY fechaCreacion DESC")
    fun getAllColecciones(): Flow<List<Coleccion>>

    // Obtiene una colección por ID
    @Query("SELECT * FROM Coleccion WHERE id = :id")
    suspend fun getColeccionById(id: Int): Coleccion?

    // Obtiene todas las colecciones activas en una sola llamada (sin Flow)
    @Query("SELECT * FROM Coleccion WHERE eliminado = 0 ORDER BY fechaCreacion DESC")
    suspend fun getAllColeccionesOnce(): List<Coleccion>

    // Cuenta el número de colecciones activas
    @Query("SELECT COUNT(*) FROM Coleccion WHERE eliminado = 0")
    suspend fun countColecciones(): Int

    // ─────────────────────────────────────────────────────────────
    // PAPELERA
    // ─────────────────────────────────────────────────────────────

    // Obtiene colecciones eliminadas (papelera), ordenadas por fecha de eliminación
    @Query("SELECT * FROM Coleccion WHERE eliminado = 1 ORDER BY fechaEliminacion DESC")
    fun getColeccionesEliminadas(): Flow<List<Coleccion>>

    // Vacía la papelera de colecciones (eliminación física local)
    @Query("DELETE FROM Coleccion WHERE eliminado = 1")
    suspend fun deleteAllEliminadas()

    // Elimina definitivamente colecciones antiguas de la papelera
    @Query("DELETE FROM Coleccion WHERE eliminado = 1 AND fechaEliminacion < :fecha")
    suspend fun limpiarColeccionesAntiguas(fecha: Long)

    // Busca colecciones activas por nombre o descripción
    @Query("""
        SELECT * FROM Coleccion 
        WHERE eliminado = 0 
        AND (nombre LIKE '%' || :search || '%' 
        OR descripcion LIKE '%' || :search || '%')
        ORDER BY fechaCreacion DESC
    """)
    suspend fun searchColecciones(search: String): List<Coleccion>
}
