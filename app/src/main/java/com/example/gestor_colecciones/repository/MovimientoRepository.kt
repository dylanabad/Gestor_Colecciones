package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.MovimientoDao
import com.example.gestor_colecciones.entities.Movimiento
import kotlinx.coroutines.flow.Flow

class MovimientoRepository(private val movimientoDao: MovimientoDao) {

    val allMovimientos: Flow<List<Movimiento>> = movimientoDao.getAllMovimientos()

    suspend fun insert(movimiento: Movimiento) = movimientoDao.insert(movimiento)
    suspend fun update(movimiento: Movimiento) = movimientoDao.update(movimiento)
    suspend fun delete(movimiento: Movimiento) = movimientoDao.delete(movimiento)

    fun getMovimientosByItem(itemId: Int): Flow<List<Movimiento>> =
        movimientoDao.getMovimientosByItem(itemId)

    fun getMovimientosByPersona(personaId: Int): Flow<List<Movimiento>> =
        movimientoDao.getMovimientosByPersona(personaId)
}