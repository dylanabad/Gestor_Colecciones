package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.model.ItemTagInfo
import com.example.gestor_colecciones.model.ItemTagName
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import com.example.gestor_colecciones.network.dto.toDto
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao,
    private val api: ApiService
) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag): Long {
        val saved = api.saveTag(tag.toDto())
        return tagDao.insert(saved.toEntity())
    }

    suspend fun update(tag: Tag) {
        val saved = api.saveTag(tag.toDto())
        tagDao.insert(saved.toEntity())
    }

    suspend fun delete(tag: Tag) {
        if (tag.id > 0) api.deleteTag(tag.id.toLong())
        tagDao.delete(tag)
    }

    suspend fun getAllTagsOnce(): List<Tag> = tagDao.getAllTagsOnce()

    fun getTagsForItem(itemId: Int): Flow<List<Tag>> = tagDao.getTagsForItem(itemId)

    suspend fun getTagsForItemOnce(itemId: Int): List<Tag> = tagDao.getTagsForItemOnce(itemId)

    suspend fun getTagNamesForItemsOnce(itemIds: List<Int>): List<ItemTagName> =
        if (itemIds.isEmpty()) emptyList() else tagDao.getTagNamesForItemsOnce(itemIds)

    suspend fun getTagInfoForItemsOnce(itemIds: List<Int>): List<ItemTagInfo> =
        if (itemIds.isEmpty()) emptyList() else tagDao.getTagInfoForItemsOnce(itemIds)

    fun searchTags(search: String): Flow<List<Tag>> = tagDao.searchTags(search)
}
