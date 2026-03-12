package com.example.gestor_colecciones.dao


import androidx.room.*
import com.example.gestor_colecciones.entities.ItemTag
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemTagDao {

    @Insert
    suspend fun insert(itemTag: ItemTag)

    @Delete
    suspend fun delete(itemTag: ItemTag)

    @Query("SELECT * FROM item_tags WHERE itemId = :itemId")
    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>>

    @Query("SELECT * FROM item_tags WHERE tagId = :tagId")
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>>
}