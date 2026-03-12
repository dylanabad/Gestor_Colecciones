package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM Tag ORDER BY nombre ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM Tag WHERE nombre LIKE '%' || :search || '%'")
    fun searchTags(search: String): Flow<List<Tag>>
}