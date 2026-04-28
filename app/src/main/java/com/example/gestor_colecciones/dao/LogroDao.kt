package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Logro
import kotlinx.coroutines.flow.Flow

/**
 * DAO del progreso local de logros desbloqueables.
 */
@Dao
interface LogroDao {

    // Inserta un logro; ignora si ya existe (por conflicto)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(logro: Logro)

    // Actualiza un logro existente
    @Update
    suspend fun update(logro: Logro)

    // Obtiene todos los logros ordenados por fecha de desbloqueo (desc)
    @Query("SELECT * FROM Logro ORDER BY fechaDesbloqueo DESC")
    fun getAllLogros(): Flow<List<Logro>>

    // Obtiene un logro específico por su clave
    @Query("SELECT * FROM Logro WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): Logro?

    // Cuenta cuántos logros han sido desbloqueados
    @Query("SELECT COUNT(*) FROM Logro WHERE desbloqueado = 1")
    suspend fun countDesbloqueados(): Int
}