package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDeseoDao
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repositorio encargado de gestionar la lista de deseos (wishlist)
 * sincronizando datos entre la base de datos local y la API remota
 */
class ItemDeseoRepository(
    private val dao: ItemDeseoDao, // Acceso a base de datos local
    private val api: ApiService    // Acceso a API remota
) {

    /**
     * Flujo con todos los items de deseo
     */
    val all: Flow<List<ItemDeseo>> = dao.getAll()

    /**
     * Flujo con los items pendientes (no conseguidos)
     */
    val pendientes: Flow<List<ItemDeseo>> = dao.getPendientes()

    /**
     * Flujo con los items ya conseguidos
     */
    val conseguidos: Flow<List<ItemDeseo>> = dao.getConseguidos()

    /**
     * Flujo con el número de items pendientes
     */
    val countPendientes: Flow<Int> = dao.countPendientes()

    /**
     * Inserta un item de deseo sincronizando con la API
     */
    suspend fun insert(item: ItemDeseo) =
        dao.insert(api.saveDeseo(item.toDto()).toEntity())

    /**
     * Actualiza un item tanto en la API como en la base de datos local
     */
    suspend fun update(item: ItemDeseo) {

        /**
         * Guarda cambios en la API
         */
        val saved = api.saveDeseo(item.toDto())

        // Inserta/actualiza en local la versión sincronizada
        dao.insert(saved.toEntity())
    }

    /**
     * Elimina un item de deseo tanto en la API como en local
     */
    suspend fun delete(item: ItemDeseo) {

        // Soft delete: a papelera (backend + local)
        if (item.id > 0) api.deleteDeseo(item.id.toLong())
        dao.update(
            item.copy(
                eliminado = true,
                fechaEliminacion = Date()
            )
        )
    }

    /**
     * Eliminación definitiva (solo se usa desde Papelera)
     */
    suspend fun deleteHard(item: ItemDeseo) {
        if (item.id > 0) api.deleteDeseoHard(item.id.toLong())
        dao.delete(item)
    }

    /**
     * Marca un item como conseguido actualizando su estado y fecha
     */
    suspend fun marcarConseguido(item: ItemDeseo) {

        update(
            item.copy(
                conseguido = true,
                fechaConseguido = Date()
            )
        )
    }
}
