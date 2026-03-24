package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    // Solo items activos
    @Query("SELECT * FROM Item WHERE eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE id = :id")
    suspend fun getItemById(id: Int): Item?

    @Query("SELECT * FROM Item WHERE collectionId = :collectionId AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getItemsByCollection(collectionId: Int): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE categoriaId = :categoriaId AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE titulo LIKE '%' || :search || '%' AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun searchItemsByTitle(search: String): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM Item WHERE eliminado = 0")
    suspend fun getTotalItems(): Int

    @Query("SELECT SUM(valor) FROM Item WHERE eliminado = 0")
    suspend fun getTotalValor(): Double?

    @Query("SELECT COUNT(*) FROM Item WHERE collectionId = :collectionId AND eliminado = 0")
    suspend fun countItemsByCollection(collectionId: Int): Int

    @Query("SELECT SUM(valor) FROM Item WHERE collectionId = :collectionId AND eliminado = 0")
    suspend fun getTotalValueByCollection(collectionId: Int): Double?

    @Query("SELECT * FROM Item WHERE collectionId = :id AND eliminado = 0")
    suspend fun getItemsByCollectionOnce(id: Int): List<Item>

    // ── Papelera ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM Item WHERE eliminado = 1 ORDER BY fechaEliminacion DESC")
    fun getItemsEliminados(): Flow<List<Item>>

    @Query("DELETE FROM Item WHERE eliminado = 1 AND fechaEliminacion < :fecha")
    suspend fun limpiarItemsAntiguos(fecha: Long)

    @Query("SELECT * FROM Item WHERE eliminado = 0 AND (titulo LIKE '%' || :search || '%' OR descripcion LIKE '%' || :search || '%' OR estado LIKE '%' || :search || '%') ORDER BY fechaAdquisicion DESC")
    suspend fun searchItems(search: String): List<Item>
}
