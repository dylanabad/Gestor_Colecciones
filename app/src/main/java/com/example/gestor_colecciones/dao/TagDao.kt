package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.model.ItemTagInfo
import com.example.gestor_colecciones.model.ItemTagName
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<Tag>)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM Tag ORDER BY nombre ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM Tag ORDER BY nombre ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    @Query(
        """
        SELECT Tag.* FROM Tag
        INNER JOIN item_tags ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId = :itemId
        ORDER BY Tag.nombre ASC
        """
    )
    fun getTagsForItem(itemId: Int): Flow<List<Tag>>

    @Query(
        """
        SELECT Tag.* FROM Tag
        INNER JOIN item_tags ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId = :itemId
        ORDER BY Tag.nombre ASC
        """
    )
    suspend fun getTagsForItemOnce(itemId: Int): List<Tag>

    @Query(
        """
        SELECT item_tags.itemId as itemId, Tag.nombre as nombre
        FROM item_tags
        INNER JOIN Tag ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId IN (:itemIds)
        ORDER BY Tag.nombre ASC
        """
    )
    suspend fun getTagNamesForItemsOnce(itemIds: List<Int>): List<ItemTagName>

    @Query(
        """
        SELECT item_tags.itemId as itemId, Tag.id as tagId, Tag.nombre as nombre
        FROM item_tags
        INNER JOIN Tag ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId IN (:itemIds)
        ORDER BY Tag.nombre ASC
        """
    )
    suspend fun getTagInfoForItemsOnce(itemIds: List<Int>): List<ItemTagInfo>

    @Query("SELECT * FROM Tag WHERE nombre LIKE '%' || :search || '%'")
    fun searchTags(search: String): Flow<List<Tag>>
}
