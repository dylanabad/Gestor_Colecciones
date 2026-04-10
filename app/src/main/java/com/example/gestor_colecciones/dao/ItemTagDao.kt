package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.ItemTag
import kotlinx.coroutines.flow.Flow

// DAO encargado de la relación entre Items y Tags (tabla intermedia)
@Dao
interface ItemTagDao {

    // Inserta una relación item-tag; ignora si ya existe
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(itemTag: ItemTag)

    // Inserta una lista de relaciones item-tag
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ItemTag>)

    // Elimina una relación específica item-tag
    @Delete
    suspend fun delete(itemTag: ItemTag)

    // Elimina todas las relaciones de un item concreto
    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Int)

    // Obtiene todos los tags asociados a un item
    @Query("SELECT * FROM item_tags WHERE itemId = :itemId")
    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>>

    // Obtiene todos los items asociados a un tag
    @Query("SELECT * FROM item_tags WHERE tagId = :tagId")
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>>
}