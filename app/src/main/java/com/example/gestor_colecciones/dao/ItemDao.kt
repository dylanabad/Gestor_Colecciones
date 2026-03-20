package com.example.gestor_colecciones.dao


import androidx.room.*
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM Item ORDER BY fechaAdquisicion DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE id = :id")
    suspend fun getItemById(id: Int): Item?

    @Query("SELECT * FROM Item WHERE collectionId = :collectionId ORDER BY fechaAdquisicion DESC")
    fun getItemsByCollection(collectionId: Int): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE categoriaId = :categoriaId ORDER BY fechaAdquisicion DESC")
    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE titulo LIKE '%' || :search || '%' ORDER BY fechaAdquisicion DESC")
    fun searchItemsByTitle(search: String): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM Item")
    suspend fun getTotalItems(): Int

    @Query("SELECT SUM(valor) FROM Item")
    suspend fun getTotalValor(): Double?

    @Query("SELECT COUNT(*) FROM item WHERE collectionId = :collectionId")
    suspend fun countItemsByCollection(collectionId: Int): Int

    @Query("SELECT SUM(valor) FROM item WHERE collectionId = :collectionId")
    suspend fun getTotalValueByCollection(collectionId: Int): Double?

    @Query("SELECT * FROM Item WHERE collectionId = :id")
    suspend fun getItemsByCollectionOnce(id: Int): List<Item>

}