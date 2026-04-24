package com.example.gestor_colecciones.dao

import androidx.room.*
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.model.ItemTagInfo
import com.example.gestor_colecciones.model.ItemTagName
import kotlinx.coroutines.flow.Flow

/**
 * DAO para etiquetas definidas por el usuario.
 */
@Dao
interface TagDao {

    // Inserta una etiqueta; reemplaza si ya existe conflicto
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag): Long

    // Inserta una lista de etiquetas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<Tag>)

    // Actualiza una etiqueta existente
    @Update
    suspend fun update(tag: Tag)

    // Elimina una etiqueta
    @Delete
    suspend fun delete(tag: Tag)

    // Obtiene todas las etiquetas ordenadas alfabéticamente
    @Query("SELECT * FROM Tag ORDER BY nombre ASC")
    fun getAllTags(): Flow<List<Tag>>

    // Obtiene todas las etiquetas una sola vez
    @Query("SELECT * FROM Tag ORDER BY nombre ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    // Obtiene las etiquetas asociadas a un item (reactivo)
    @Query(
        """
        SELECT Tag.* FROM Tag
        INNER JOIN item_tags ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId = :itemId
        ORDER BY Tag.nombre ASC
        """
    )
    fun getTagsForItem(itemId: Int): Flow<List<Tag>>

    // Obtiene las etiquetas asociadas a un item (consulta puntual)
    @Query(
        """
        SELECT Tag.* FROM Tag
        INNER JOIN item_tags ON Tag.id = item_tags.tagId
        WHERE item_tags.itemId = :itemId
        ORDER BY Tag.nombre ASC
        """
    )
    suspend fun getTagsForItemOnce(itemId: Int): List<Tag>

    // Obtiene los nombres de tags asociados a varios items
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

    // Obtiene información detallada de tags asociados a varios items
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

    // Búsqueda de etiquetas por nombre
    @Query("SELECT * FROM Tag WHERE nombre LIKE '%' || :search || '%'")
    fun searchTags(search: String): Flow<List<Tag>>
}