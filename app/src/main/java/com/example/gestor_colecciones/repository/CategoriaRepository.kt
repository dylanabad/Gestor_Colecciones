package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(
    private val categoriaDao: CategoriaDao,
    private val api: ApiService
) {

    val allCategorias: Flow<List<Categoria>> = categoriaDao.getAllCategorias()

    suspend fun insert(categoria: Categoria): Long {
        val saved = api.saveCategoria(categoria.toDto())
        return categoriaDao.insert(saved.toEntity())
    }

    suspend fun update(categoria: Categoria) {
        val saved = api.saveCategoria(categoria.toDto())
        categoriaDao.insert(saved.toEntity())
    }

    suspend fun delete(categoria: Categoria) {
        api.deleteCategoria(categoria.id.toLong())
        categoriaDao.delete(categoria)
    }

    suspend fun allCategoriasOnce(): List<Categoria> {
        return categoriaDao.getAllCategoriasOnce()
    }
}
