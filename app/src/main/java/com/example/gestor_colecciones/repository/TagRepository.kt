package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insert(tag: Tag) = tagDao.insert(tag)
    suspend fun update(tag: Tag) = tagDao.update(tag)
    suspend fun delete(tag: Tag) = tagDao.delete(tag)

    fun searchTags(search: String): Flow<List<Tag>> = tagDao.searchTags(search)
}