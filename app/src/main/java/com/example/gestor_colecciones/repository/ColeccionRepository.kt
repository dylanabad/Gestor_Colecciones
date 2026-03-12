package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.entities.Coleccion
import kotlinx.coroutines.flow.Flow

class ColeccionRepository(private val coleccionDao: ColeccionDao) {

    val allColecciones: Flow<List<Coleccion>> = coleccionDao.getAllColecciones()

    suspend fun insert(coleccion: Coleccion) = coleccionDao.insert(coleccion)
    suspend fun update(coleccion: Coleccion) = coleccionDao.update(coleccion)
    suspend fun delete(coleccion: Coleccion) = coleccionDao.delete(coleccion)
    suspend fun getById(id: Int): Coleccion? = coleccionDao.getColeccionById(id)
}