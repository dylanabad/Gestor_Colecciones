package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.database.AppDataBase
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sincroniza la cache Room con el estado actual del backend.
 *
 * La estrategia es de reconstruccion completa: limpia las tablas locales y las
 * repuebla con la informacion remota para evitar inconsistencias entre usuarios o sesiones.
 */
class SyncRepository(
    private val api: ApiService,
    private val db: AppDataBase
) {

    /**
     * Ejecuta una sincronizacion total de categorias, colecciones, items,
     * etiquetas, relaciones item-tag y lista de deseos.
     */
    suspend fun syncAll() {
        withContext(Dispatchers.IO) {
            db.clearAllTables()

            val categorias = api.getCategorias().map { it.toEntity() }
            if (categorias.isNotEmpty()) {
                db.categoriaDao().insertAll(categorias)
            }

            val coleccionesActivas = api.getColecciones().map { it.toEntity() }
            val coleccionesEliminadas = api.getColeccionesEliminadas().map { it.toEntity() }
            val allColecciones = coleccionesActivas + coleccionesEliminadas
            if (allColecciones.isNotEmpty()) {
                db.coleccionDao().insertAll(allColecciones)
            }

            val allItems = mutableListOf<com.example.gestor_colecciones.entities.Item>()
            for (coleccion in coleccionesActivas) {
                val items = api.getItemsByColeccion(coleccion.id.toLong())
                    .map { it.toEntity(coleccion.id) }
                allItems.addAll(items)
            }

            for (coleccion in allColecciones) {
                val itemsEliminados = api.getItemsEliminadosByColeccion(coleccion.id.toLong())
                    .map { it.toEntity(coleccion.id) }
                allItems.addAll(itemsEliminados)
            }

            if (allItems.isNotEmpty()) {
                db.itemDao().insertAll(allItems)
            }

            val tags = api.getTags().map { it.toEntity() }
            if (tags.isNotEmpty()) {
                db.tagDao().insertAll(tags)
            }

            val itemTags = mutableListOf<ItemTag>()
            for (item in allItems) {
                try {
                    val tagsForItem = api.getItemTags(item.id.toLong())
                    tagsForItem.forEach { dto ->
                        val tagId = dto.id?.toInt() ?: return@forEach
                        itemTags.add(ItemTag(itemId = item.id, tagId = tagId))
                    }
                } catch (e: Exception) {
                    // Se continua con el resto para no abortar la sincronizacion por un item aislado.
                    e.printStackTrace()
                }
            }

            if (itemTags.isNotEmpty()) {
                db.itemTagDao().insertAll(itemTags)
            }

            val deseos = api.getDeseos().map { it.toEntity() }
            val deseosEliminados = api.getDeseosEliminados().map { it.toEntity() }
            val allDeseos = deseos + deseosEliminados
            if (allDeseos.isNotEmpty()) {
                db.itemDeseoDao().insertAll(allDeseos)
            }
        }
    }
}
