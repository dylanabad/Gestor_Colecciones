package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.entities.ItemTag
import kotlinx.coroutines.flow.Flow

class ItemTagRepository(private val itemTagDao: ItemTagDao) {

    suspend fun insert(itemTag: ItemTag) = itemTagDao.insert(itemTag)
    suspend fun delete(itemTag: ItemTag) = itemTagDao.delete(itemTag)

    suspend fun replaceTagsForItem(itemId: Int, tagIds: List<Int>) {
        itemTagDao.deleteAllForItem(itemId)
        tagIds.distinct().forEach { tagId ->
            itemTagDao.insert(ItemTag(itemId = itemId, tagId = tagId))
        }
    }

    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>> = itemTagDao.getTagsForItem(itemId)
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>> = itemTagDao.getItemsForTag(tagId)
}
