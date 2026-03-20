package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDeseoDao
import com.example.gestor_colecciones.entities.ItemDeseo
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ItemDeseoRepository(private val dao: ItemDeseoDao) {

    val all: Flow<List<ItemDeseo>> = dao.getAll()
    val pendientes: Flow<List<ItemDeseo>> = dao.getPendientes()
    val conseguidos: Flow<List<ItemDeseo>> = dao.getConseguidos()
    val countPendientes: Flow<Int> = dao.countPendientes()

    suspend fun insert(item: ItemDeseo) = dao.insert(item)
    suspend fun update(item: ItemDeseo) = dao.update(item)
    suspend fun delete(item: ItemDeseo) = dao.delete(item)

    suspend fun marcarConseguido(item: ItemDeseo) {
        dao.update(item.copy(conseguido = true, fechaConseguido = Date()))
    }
}