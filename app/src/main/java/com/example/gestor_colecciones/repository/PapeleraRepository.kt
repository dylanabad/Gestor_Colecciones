package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

class PapeleraRepository(
    private val coleccionDao: ColeccionDao,
    private val itemDao: ItemDao,
    private val api: ApiService
) {
    val coleccionesEliminadas: Flow<List<Coleccion>> = coleccionDao.getColeccionesEliminadas()
    val itemsEliminados: Flow<List<Item>> = itemDao.getItemsEliminados()

    suspend fun moverColeccionAPapelera(coleccion: Coleccion) {
        api.deleteColeccion(coleccion.id.toLong())
        coleccionDao.update(coleccion.copy(eliminado = true, fechaEliminacion = Date()))
    }

    suspend fun moverItemAPapelera(item: Item) {
        api.saveItem(
            item.collectionId.toLong(),
            item.categoriaId.takeIf { it > 0 }?.toLong(),
            item.copy(eliminado = true, fechaEliminacion = Date()).toDto()
        )
        itemDao.update(item.copy(eliminado = true, fechaEliminacion = Date()))
    }

    suspend fun restaurarColeccion(coleccion: Coleccion) {
        api.saveColeccion(coleccion.copy(eliminado = false, fechaEliminacion = null).toDto())
        coleccionDao.update(coleccion.copy(eliminado = false, fechaEliminacion = null))
    }

    suspend fun restaurarItem(item: Item) {
        api.saveItem(
            item.collectionId.toLong(),
            item.categoriaId.takeIf { it > 0 }?.toLong(),
            item.copy(eliminado = false, fechaEliminacion = null).toDto()
        )
        itemDao.update(item.copy(eliminado = false, fechaEliminacion = null))
    }

    suspend fun eliminarColeccionDefinitivamente(coleccion: Coleccion) {
        api.deleteColeccion(coleccion.id.toLong())
        coleccionDao.delete(coleccion)
    }

    suspend fun eliminarItemDefinitivamente(item: Item) {
        api.deleteItem(item.id.toLong())
        itemDao.delete(item)
    }

    suspend fun limpiarElementosAntiguos() {
        val hace30Dias = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis
        coleccionDao.limpiarColeccionesAntiguas(hace30Dias)
        itemDao.limpiarItemsAntiguos(hace30Dias)
    }
}
