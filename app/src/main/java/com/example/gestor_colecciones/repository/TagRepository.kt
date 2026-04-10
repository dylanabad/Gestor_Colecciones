package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.model.ItemTagInfo
import com.example.gestor_colecciones.model.ItemTagName
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import com.example.gestor_colecciones.network.dto.toDto
import kotlinx.coroutines.flow.Flow

// Repositorio encargado de gestionar los tags (etiquetas) tanto en local como en remoto
class TagRepository(
    private val tagDao: TagDao,   // Acceso a base de datos local de tags
    private val api: ApiService   // Acceso a API remota
) {

    // Flujo con todos los tags almacenados localmente
    val allTags: Flow<List<Tag>> =
        tagDao.getAllTags()

    // Inserta un tag en la API y lo guarda en local
    suspend fun insert(tag: Tag): Long {

        // Guarda el tag en el servidor
        val saved = api.saveTag(tag.toDto())

        // Inserta la versión sincronizada en local
        return tagDao.insert(saved.toEntity())
    }

    // Actualiza un tag en la API y en la base de datos local
    suspend fun update(tag: Tag) {

        // Envía cambios al servidor
        val saved = api.saveTag(tag.toDto())

        // Actualiza/inserta en local
        tagDao.insert(saved.toEntity())
    }

    // Elimina un tag en remoto y local
    suspend fun delete(tag: Tag) {

        // Solo intenta borrar en API si el tag ya existe en servidor
        if (tag.id > 0) api.deleteTag(tag.id.toLong())

        // Elimina en base de datos local
        tagDao.delete(tag)
    }

    // Obtiene todos los tags en una sola consulta (no reactivo)
    suspend fun getAllTagsOnce(): List<Tag> =
        tagDao.getAllTagsOnce()

    // Obtiene tags asociados a un item como Flow reactivo
    fun getTagsForItem(itemId: Int): Flow<List<Tag>> =
        tagDao.getTagsForItem(itemId)

    // Obtiene tags asociados a un item en una sola consulta
    suspend fun getTagsForItemOnce(itemId: Int): List<Tag> =
        tagDao.getTagsForItemOnce(itemId)

    // Obtiene nombres de tags asociados a múltiples items (batch query)
    suspend fun getTagNamesForItemsOnce(itemIds: List<Int>): List<ItemTagName> =
        if (itemIds.isEmpty()) emptyList()
        else tagDao.getTagNamesForItemsOnce(itemIds)

    // Obtiene información completa de tags asociados a múltiples items
    suspend fun getTagInfoForItemsOnce(itemIds: List<Int>): List<ItemTagInfo> =
        if (itemIds.isEmpty()) emptyList()
        else tagDao.getTagInfoForItemsOnce(itemIds)

    // Búsqueda de tags por texto
    fun searchTags(search: String): Flow<List<Tag>> =
        tagDao.searchTags(search)
}