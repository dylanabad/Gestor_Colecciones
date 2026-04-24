package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio encargado de gestionar la relación muchos-a-muchos entre Items y Tags
 */
class ItemTagRepository(
    private val itemTagDao: ItemTagDao, // Acceso a tabla intermedia item-tag
    private val tagDao: TagDao,         // Acceso a tabla de tags
    private val api: ApiService         // Acceso a API remota
) {

    /**
     * Inserta una relación item-tag en base de datos local
     */
    suspend fun insert(itemTag: ItemTag) =
        itemTagDao.insert(itemTag)

    /**
     * Elimina una relación item-tag en base de datos local
     */
    suspend fun delete(itemTag: ItemTag) =
        itemTagDao.delete(itemTag)

    /**
     * Reemplaza completamente los tags de un item
     */
    suspend fun replaceTagsForItem(itemId: Int, tagIds: List<Int>) {

        /**
         * Envía la lista de tags al servidor (sin duplicados)
         */
        val saved = api.setItemTags(
            itemId.toLong(),
            tagIds.distinct().map { it.toLong() }
        )

        // Si el servidor devuelve tags, se guardan en local
        if (saved.isNotEmpty()) {
            tagDao.insertAll(saved.map { it.toEntity() })
        }

        // Borra relaciones antiguas del item en local
        itemTagDao.deleteAllForItem(itemId)

        /**
         * Reconstruye las relaciones item-tag desde la respuesta del servidor
         */
        val links = saved.mapNotNull { dto ->
            dto.id?.toInt()?.let { tagId ->
                ItemTag(itemId = itemId, tagId = tagId)
            }
        }

        // Inserta nuevas relaciones si existen
        if (links.isNotEmpty()) itemTagDao.insertAll(links)
    }

    /**
     * Sincroniza los tags de un item desde el servidor
     */
    suspend fun syncTagsForItem(itemId: Int) {

        /**
         * Obtiene tags desde la API
         */
        val tags = api.getItemTags(itemId.toLong())

        // Guarda tags en base de datos local
        if (tags.isNotEmpty()) {
            tagDao.insertAll(tags.map { it.toEntity() })
        }

        // Elimina relaciones antiguas locales
        itemTagDao.deleteAllForItem(itemId)

        /**
         * Reconstruye relaciones item-tag
         */
        val links = tags.mapNotNull { dto ->
            dto.id?.toInt()?.let { tagId ->
                ItemTag(itemId = itemId, tagId = tagId)
            }
        }

        // Inserta relaciones nuevas si existen
        if (links.isNotEmpty()) itemTagDao.insertAll(links)
    }

    /**
     * Obtiene los tags asociados a un item como flujo reactivo
     */
    fun getTagsForItem(itemId: Int): Flow<List<ItemTag>> =
        itemTagDao.getTagsForItem(itemId)

    /**
     * Obtiene los items asociados a un tag como flujo reactivo
     */
    fun getItemsForTag(tagId: Int): Flow<List<ItemTag>> =
        itemTagDao.getItemsForTag(tagId)
}