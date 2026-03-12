package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.entities.Categoria
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(private val categoriaDao: CategoriaDao) {

    val allCategorias: Flow<List<Categoria>> = categoriaDao.getAllCategorias()

    suspend fun insert(categoria: Categoria) = categoriaDao.insert(categoria)
    suspend fun update(categoria: Categoria) = categoriaDao.update(categoria)
    suspend fun delete(categoria: Categoria) = categoriaDao.delete(categoria)
}