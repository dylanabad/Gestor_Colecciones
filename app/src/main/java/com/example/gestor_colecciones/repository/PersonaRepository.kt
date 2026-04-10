package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.PersonaDao
import com.example.gestor_colecciones.entities.Persona
import kotlinx.coroutines.flow.Flow

// Repositorio encargado de gestionar las personas en la base de datos local
class PersonaRepository(
    private val personaDao: PersonaDao // Acceso a la tabla de personas
) {

    // Flujo reactivo con todas las personas almacenadas
    val allPersonas: Flow<List<Persona>> =
        personaDao.getAllPersonas()

    // Inserta una nueva persona en la base de datos
    suspend fun insert(persona: Persona) =
        personaDao.insert(persona)

    // Actualiza una persona existente
    suspend fun update(persona: Persona) =
        personaDao.update(persona)

    // Elimina una persona de la base de datos
    suspend fun delete(persona: Persona) =
        personaDao.delete(persona)
}