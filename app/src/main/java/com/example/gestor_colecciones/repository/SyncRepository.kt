package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.database.AppDataBase
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toEntity
import com.example.gestor_colecciones.entities.ItemTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repositorio encargado de sincronizar toda la base de datos local con el backend
class SyncRepository(
    private val api: ApiService,     // API remota
    private val db: AppDataBase      // Base de datos local
) {

    // Sincroniza completamente el estado del servidor con la base de datos local
    suspend fun syncAll() {

        // Ejecuta toda la sincronización en el hilo de IO
        withContext(Dispatchers.IO) {

            // Limpia todas las tablas locales antes de sincronizar
            db.clearAllTables()

            // Sincroniza categorías desde el servidor
            val categorias = api.getCategorias().map { it.toEntity() }
            if (categorias.isNotEmpty()) {
                db.categoriaDao().insertAll(categorias)
            }

            // Sincroniza colecciones desde el servidor (activas + eliminadas/papelera)
            val coleccionesActivas = api.getColecciones().map { it.toEntity() }
            val coleccionesEliminadas = api.getColeccionesEliminadas().map { it.toEntity() }
            val allColecciones = coleccionesActivas + coleccionesEliminadas

            if (allColecciones.isNotEmpty()) {
                db.coleccionDao().insertAll(allColecciones)
            }

            // Sincroniza items por colección
            val allItems = mutableListOf<com.example.gestor_colecciones.entities.Item>()

            // Activos: solo de colecciones activas
            for (coleccion in coleccionesActivas) {

                val items = api.getItemsByColeccion(coleccion.id.toLong())
                    .map { it.toEntity(coleccion.id) }

                allItems.addAll(items)
            }

            // Papelera: items eliminados (incluye items eliminados de colecciones activas o eliminadas)
            for (coleccion in allColecciones) {

                val itemsEliminados = api.getItemsEliminadosByColeccion(coleccion.id.toLong())
                    .map { it.toEntity(coleccion.id) }

                allItems.addAll(itemsEliminados)
            }

            if (allItems.isNotEmpty()) {
                db.itemDao().insertAll(allItems)
            }

            // Sincroniza tags desde el servidor
            val tags = api.getTags().map { it.toEntity() }
            if (tags.isNotEmpty()) {
                db.tagDao().insertAll(tags)
            }

            // Sincroniza relaciones item-tag
            val itemTags = mutableListOf<ItemTag>()

            for (item in allItems) {

                val tagsForItem = api.getItemTags(item.id.toLong())

                tagsForItem.forEach { dto ->

                    val tagId = dto.id?.toInt() ?: return@forEach

                    itemTags.add(
                        ItemTag(
                            itemId = item.id,
                            tagId = tagId
                        )
                    )
                }
            }

            if (itemTags.isNotEmpty()) {
                db.itemTagDao().insertAll(itemTags)
            }

            // Sincroniza lista de deseos
            val deseos = api.getDeseos().map { it.toEntity() }
            val deseosEliminados = api.getDeseosEliminados().map { it.toEntity() }
            val allDeseos = deseos + deseosEliminados
            if (allDeseos.isNotEmpty()) {
                db.itemDeseoDao().insertAll(allDeseos)
            }
        }
    }
}
