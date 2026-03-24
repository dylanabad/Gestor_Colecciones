package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: Categoria): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<Categoria>)

    @Update
    suspend fun update(categoria: Categoria)

    @Delete
    suspend fun delete(categoria: Categoria)

    @Query("SELECT * FROM Categoria ORDER BY nombre ASC")
    fun getAllCategorias(): Flow<List<Categoria>>


    @Query("SELECT * FROM Categoria")
    suspend fun getAllCategoriasOnce(): List<Categoria>
}
