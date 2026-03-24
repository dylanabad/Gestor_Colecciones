package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDeseoDao
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ItemDeseoRepository(
    private val dao: ItemDeseoDao,
    private val api: ApiService
) {

    val all: Flow<List<ItemDeseo>> = dao.getAll()
    val pendientes: Flow<List<ItemDeseo>> = dao.getPendientes()
    val conseguidos: Flow<List<ItemDeseo>> = dao.getConseguidos()
    val countPendientes: Flow<Int> = dao.countPendientes()

    suspend fun insert(item: ItemDeseo) = dao.insert(api.saveDeseo(item.toDto()).toEntity())

    suspend fun update(item: ItemDeseo) {
        val saved = api.saveDeseo(item.toDto())
        dao.insert(saved.toEntity())
    }

    suspend fun delete(item: ItemDeseo) {
        if (item.id > 0) api.deleteDeseo(item.id.toLong())
        dao.delete(item)
    }

    suspend fun marcarConseguido(item: ItemDeseo) {
        update(item.copy(conseguido = true, fechaConseguido = Date()))
    }
}
