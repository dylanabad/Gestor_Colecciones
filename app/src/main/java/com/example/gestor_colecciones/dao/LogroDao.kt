package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Logro
import kotlinx.coroutines.flow.Flow

@Dao
interface LogroDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(logro: Logro)

    @Update
    suspend fun update(logro: Logro)

    @Query("SELECT * FROM Logro ORDER BY fechaDesbloqueo DESC")
    fun getAllLogros(): Flow<List<Logro>>

    @Query("SELECT * FROM Logro WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): Logro?

    @Query("SELECT COUNT(*) FROM Logro WHERE desbloqueado = 1")
    suspend fun countDesbloqueados(): Int
}