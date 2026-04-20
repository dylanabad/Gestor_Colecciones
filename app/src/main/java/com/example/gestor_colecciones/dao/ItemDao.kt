package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.Flow

// DAO encargado de gestionar el acceso a datos de la entidad Item
@Dao
interface ItemDao {

    // Inserta un item; si hay conflicto lo ignora para evitar borrar registros relacionados
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Item): Long

    // Inserta una lista de items ignorando conflictos
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Item>)

    // Actualiza un item existente
    @Update
    suspend fun update(item: Item)

    // Elimina un item de la base de datos
    @Delete
    suspend fun delete(item: Item)

    // ─────────────────────────────────────────────────────────────
    // ITEMS ACTIVOS
    // ─────────────────────────────────────────────────────────────

    // Obtiene todos los items activos ordenados por fecha de adquisición (desc)
    @Query("SELECT * FROM Item WHERE eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getAllItems(): Flow<List<Item>>

    // Obtiene un item por su ID
    @Query("SELECT * FROM Item WHERE id = :id")
    suspend fun getItemById(id: Int): Item?

    @Query("UPDATE Item SET prestado = :prestado WHERE id = :itemId")
    suspend fun updatePrestadoStatus(itemId: Int, prestado: Boolean)

    // Obtiene items activos de una colección específica
    @Query("SELECT * FROM Item WHERE collectionId = :collectionId AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getItemsByCollection(collectionId: Int): Flow<List<Item>>

    // Obtiene items activos por categoría
    @Query("SELECT * FROM Item WHERE categoriaId = :categoriaId AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun getItemsByCategoria(categoriaId: Int): Flow<List<Item>>

    // Busca items por título dentro de los activos
    @Query("SELECT * FROM Item WHERE titulo LIKE '%' || :search || '%' AND eliminado = 0 ORDER BY fechaAdquisicion DESC")
    fun searchItemsByTitle(search: String): Flow<List<Item>>

    // Cuenta total de items activos
    @Query("SELECT COUNT(*) FROM Item WHERE eliminado = 0")
    suspend fun getTotalItems(): Int

    // Suma del valor total de items activos
    @Query("SELECT SUM(valor) FROM Item WHERE eliminado = 0")
    suspend fun getTotalValor(): Double?

    // Cuenta items de una colección específica
    @Query("SELECT COUNT(*) FROM Item WHERE collectionId = :collectionId AND eliminado = 0")
    suspend fun countItemsByCollection(collectionId: Int): Int

    // Valor total de items en una colección específica
    @Query("SELECT SUM(valor) FROM Item WHERE collectionId = :collectionId AND eliminado = 0")
    suspend fun getTotalValueByCollection(collectionId: Int): Double?

    // Obtiene items de una colección en una sola llamada (sin Flow)
    @Query("SELECT * FROM Item WHERE collectionId = :id AND eliminado = 0")
    suspend fun getItemsByCollectionOnce(id: Int): List<Item>

    // ─────────────────────────────────────────────────────────────
    // PAPELERA
    // ─────────────────────────────────────────────────────────────

    // Obtiene items eliminados (papelera), ordenados por fecha de eliminación
    @Query("SELECT * FROM Item WHERE eliminado = 1 ORDER BY fechaEliminacion DESC")
    fun getItemsEliminados(): Flow<List<Item>>

    // Vacía la papelera de items (eliminación física local)
    @Query("DELETE FROM Item WHERE eliminado = 1")
    suspend fun deleteAllEliminados()

    // Elimina todos los items (activos o eliminados) de una colección.
    // Ãštil cuando se borra definitivamente una colección desde la papelera.
    @Query("DELETE FROM Item WHERE collectionId = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    // Elimina definitivamente items antiguos de la papelera
    @Query("DELETE FROM Item WHERE eliminado = 1 AND fechaEliminacion < :fecha")
    suspend fun limpiarItemsAntiguos(fecha: Long)

    // Búsqueda en items activos por varios campos
    @Query("""
        SELECT * FROM Item 
        WHERE eliminado = 0 
        AND (
            titulo LIKE '%' || :search || '%' 
            OR descripcion LIKE '%' || :search || '%' 
            OR estado LIKE '%' || :search || '%'
        )
        ORDER BY fechaAdquisicion DESC
    """)
    suspend fun searchItems(search: String): List<Item>
}
