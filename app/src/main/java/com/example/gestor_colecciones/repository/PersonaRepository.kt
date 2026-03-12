package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.PersonaDao
import com.example.gestor_colecciones.entities.Persona
import kotlinx.coroutines.flow.Flow

class PersonaRepository(private val personaDao: PersonaDao) {

    val allPersonas: Flow<List<Persona>> = personaDao.getAllPersonas()

    suspend fun insert(persona: Persona) = personaDao.insert(persona)
    suspend fun update(persona: Persona) = personaDao.update(persona)
    suspend fun delete(persona: Persona) = personaDao.delete(persona)
}