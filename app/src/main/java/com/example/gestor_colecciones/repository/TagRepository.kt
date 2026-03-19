package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.model.ItemTagInfo
import com.example.gestor_colecciones.model.ItemTagName
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag) = tagDao.insert(tag)
    suspend fun update(tag: Tag) = tagDao.update(tag)
    suspend fun delete(tag: Tag) = tagDao.delete(tag)

    suspend fun getAllTagsOnce(): List<Tag> = tagDao.getAllTagsOnce()

    fun getTagsForItem(itemId: Int): Flow<List<Tag>> = tagDao.getTagsForItem(itemId)

    suspend fun getTagsForItemOnce(itemId: Int): List<Tag> = tagDao.getTagsForItemOnce(itemId)

    suspend fun getTagNamesForItemsOnce(itemIds: List<Int>): List<ItemTagName> =
        if (itemIds.isEmpty()) emptyList() else tagDao.getTagNamesForItemsOnce(itemIds)

    suspend fun getTagInfoForItemsOnce(itemIds: List<Int>): List<ItemTagInfo> =
        if (itemIds.isEmpty()) emptyList() else tagDao.getTagInfoForItemsOnce(itemIds)

    fun searchTags(search: String): Flow<List<Tag>> = tagDao.searchTags(search)
}
