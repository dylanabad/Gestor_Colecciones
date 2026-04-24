package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que gestiona el acceso a categorías tanto local (Room) como remoto (API)
 */
class CategoriaRepository(
    private val categoriaDao: CategoriaDao, // Acceso a base de datos local
    private val api: ApiService             // Acceso a API remota
) {

    /**
     * Flujo reactivo con todas las categorías almacenadas localmente
     */
    val allCategorias: Flow<List<Categoria>> = categoriaDao.getAllCategorias()

    /**
     * Inserta una categoría: primero en la API y luego en la base de datos local
     */
    suspend fun insert(categoria: Categoria): Long {

        /**
         * Envía la categoría al servidor y recibe la versión guardada
         */
        val saved = api.saveCategoria(categoria.toDto())

        // Inserta la versión sincronizada en la base de datos local
        return categoriaDao.insert(saved.toEntity())
    }

    /**
     * Actualiza una categoría tanto en la API como en la base de datos local
     */
    suspend fun update(categoria: Categoria) {

        /**
         * Guarda cambios en la API
         */
        val saved = api.saveCategoria(categoria.toDto())

        // Actualiza localmente (insert puede actuar como upsert según el DAO)
        categoriaDao.insert(saved.toEntity())
    }

    /**
     * Elimina una categoría tanto del servidor como de la base de datos local
     */
    suspend fun delete(categoria: Categoria) {

        // Elimina en la API usando el id convertido a Long
        api.deleteCategoria(categoria.id.toLong())

        // Elimina en local
        categoriaDao.delete(categoria)
    }

    /**
     * Obtiene todas las categorías en una sola consulta (sin Flow)
     */
    suspend fun allCategoriasOnce(): List<Categoria> {
        return categoriaDao.getAllCategoriasOnce()
    }
}