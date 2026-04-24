package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Persona
import kotlinx.coroutines.flow.Flow

/**
 * DAO de personas relacionadas con prestamos o contactos del usuario.
 */
@Dao
interface PersonaDao {

    // Inserta una nueva persona y devuelve su ID generado
    @Insert
    suspend fun insert(persona: Persona): Long

    // Actualiza los datos de una persona existente
    @Update
    suspend fun update(persona: Persona)

    // Elimina una persona de la base de datos
    @Delete
    suspend fun delete(persona: Persona)

    // Obtiene todas las personas ordenadas alfabéticamente por nombre
    @Query("SELECT * FROM Persona ORDER BY nombre ASC")
    fun getAllPersonas(): Flow<List<Persona>>

    // Busca una persona por su referencia de usuario
    @Query("SELECT * FROM Persona WHERE usuarioRefId = :usuarioRefId LIMIT 1")
    suspend fun findByUsuarioRefId(usuarioRefId: Int): Persona?
}