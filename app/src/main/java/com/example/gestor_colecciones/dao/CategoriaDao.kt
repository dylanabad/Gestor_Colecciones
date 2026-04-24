package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Categoria
import kotlinx.coroutines.flow.Flow

/**
 * Define las operaciones Room para categorias activas del usuario.
 */
@Dao
interface CategoriaDao {

    // Inserta una categoría; si ya existe, la reemplaza
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: Categoria): Long

    // Inserta una lista de categorías; reemplaza en caso de conflicto
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<Categoria>)

    // Actualiza una categoría existente
    @Update
    suspend fun update(categoria: Categoria)

    // Elimina una categoría de la base de datos
    @Delete
    suspend fun delete(categoria: Categoria)

    // Obtiene todas las categorías ordenadas alfabéticamente (reactivo con Flow)
    @Query("SELECT * FROM Categoria ORDER BY nombre ASC")
    fun getAllCategorias(): Flow<List<Categoria>>

    // Obtiene todas las categorías una sola vez (sin Flow)
    @Query("SELECT * FROM Categoria")
    suspend fun getAllCategoriasOnce(): List<Categoria>
}