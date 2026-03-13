package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert
    suspend fun insert(categoria: Categoria): Long

    @Update
    suspend fun update(categoria: Categoria)

    @Delete
    suspend fun delete(categoria: Categoria)

    @Query("SELECT * FROM Categoria ORDER BY nombre ASC")
    fun getAllCategorias(): Flow<List<Categoria>>


    @Query("SELECT * FROM Categoria")
    suspend fun getAllCategoriasOnce(): List<Categoria>
    }
