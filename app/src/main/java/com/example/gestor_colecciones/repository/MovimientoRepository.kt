package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.MovimientoDao
import com.example.gestor_colecciones.entities.Movimiento
import kotlinx.coroutines.flow.Flow

// Repositorio encargado de gestionar los movimientos de los items
class MovimientoRepository(
    private val movimientoDao: MovimientoDao // Acceso a base de datos local de movimientos
) {

    // Flujo con todos los movimientos almacenados en la base de datos
    val allMovimientos: Flow<List<Movimiento>> =
        movimientoDao.getAllMovimientos()

    // Inserta un nuevo movimiento en la base de datos
    suspend fun insert(movimiento: Movimiento) =
        movimientoDao.insert(movimiento)

    // Actualiza un movimiento existente
    suspend fun update(movimiento: Movimiento) =
        movimientoDao.update(movimiento)

    // Elimina un movimiento de la base de datos
    suspend fun delete(movimiento: Movimiento) =
        movimientoDao.delete(movimiento)

    // Obtiene los movimientos asociados a un item concreto
    fun getMovimientosByItem(itemId: Int): Flow<List<Movimiento>> =
        movimientoDao.getMovimientosByItem(itemId)

    // Obtiene los movimientos asociados a una persona concreta
    fun getMovimientosByPersona(personaId: Int): Flow<List<Movimiento>> =
        movimientoDao.getMovimientosByPersona(personaId)
}