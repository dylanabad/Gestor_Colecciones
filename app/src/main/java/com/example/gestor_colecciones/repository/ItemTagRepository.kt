package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow

class ItemTagRepository(
    private val itemTagDao: ItemTagDao,
    private val tagDao: TagDao,
    private val api: ApiService
) {

    suspend fun insert(itemTag: ItemTag) = itemTagDao.insert(itemTag)
    suspend fun delete(itemTag: ItemTag) = itemTagDao.delete(itemTag)

    suspend fun replaceTagsForItem(itemId: Int, tagIds: List<Int>) {
        val saved = api.setItemTags(itemId.toLong(), tagIds.distinct().map { it.toLong() })
        if (saved.isNotEmpty()) {
            tagDao.insertAll(saved.map { it.toEntity() })
        }
        itemTagDao.deleteAllForItem(itemId)
        val links = saved.mapNotNull { dto -> dto.id?.toInt()?.let { ItemTag(itemId = itemId, tagId = it) } }
        if (links.isNotEmpty()) itemTagDao.insertAll(links)
    }

    suspend fun syncTagsForItem(itemId: Int) {
        val tags = api.getItemTags(itemId.toLong())
        if (tags.isNotEmpty()) {
            tagDao.insertAll(tags.map { it.toEntity() })
        }
        itemTagDao.deleteAllForItem(itemId)
        val links = tags.mapNotNull { dto -> dto.id?.toInt()?.let { ItemTag(itemId = itemId, tagId = it) } }
        if (links.isNotEmpty()) itemTagDao.insertAll(links)
    }

    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>> = itemTagDao.getTagsForItem(itemId)
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>> = itemTagDao.getItemsForTag(tagId)
}
