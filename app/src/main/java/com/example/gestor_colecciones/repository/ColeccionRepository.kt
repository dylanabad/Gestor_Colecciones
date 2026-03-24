package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ColeccionRepository(
    private val coleccionDao: ColeccionDao,
    private val api: ApiService
) {

    val allColecciones: Flow<List<Coleccion>> = coleccionDao.getAllColecciones()

    suspend fun insert(coleccion: Coleccion): Long {
        val saved = api.saveColeccion(coleccion.toDto())
        return coleccionDao.insert(saved.toEntity())
    }

    suspend fun update(coleccion: Coleccion) {
        val saved = api.saveColeccion(coleccion.toDto())
        coleccionDao.insert(saved.toEntity())
    }

    suspend fun delete(coleccion: Coleccion) {
        api.deleteColeccion(coleccion.id.toLong())
        coleccionDao.update(coleccion.copy(eliminado = true, fechaEliminacion = Date()))
    }

    suspend fun getById(id: Int): Coleccion? = coleccionDao.getColeccionById(id)
    suspend fun getAllOnce(): List<Coleccion> = coleccionDao.getAllColeccionesOnce()
}
