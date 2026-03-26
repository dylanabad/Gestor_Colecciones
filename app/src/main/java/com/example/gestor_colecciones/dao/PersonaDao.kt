package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Insert
    suspend fun insert(persona: Persona): Long

    @Update
    suspend fun update(persona: Persona)

    @Delete
    suspend fun delete(persona: Persona)

    @Query("SELECT * FROM Persona ORDER BY nombre ASC")
    fun getAllPersonas(): Flow<List<Persona>>

    @Query("SELECT * FROM Persona WHERE usuarioRefId = :usuarioRefId LIMIT 1")
    suspend fun findByUsuarioRefId(usuarioRefId: Int): Persona?  // ← añadido
}