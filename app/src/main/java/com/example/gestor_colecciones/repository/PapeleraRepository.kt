package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

class PapeleraRepository(
    private val coleccionDao: ColeccionDao,
    private val itemDao: ItemDao
) {
    val coleccionesEliminadas: Flow<List<Coleccion>> = coleccionDao.getColeccionesEliminadas()
    val itemsEliminados: Flow<List<Item>> = itemDao.getItemsEliminados()

    suspend fun moverColeccionAPapelera(coleccion: Coleccion) {
        coleccionDao.update(coleccion.copy(eliminado = true, fechaEliminacion = Date()))
    }

    suspend fun moverItemAPapelera(item: Item) {
        itemDao.update(item.copy(eliminado = true, fechaEliminacion = Date()))
    }

    suspend fun restaurarColeccion(coleccion: Coleccion) {
        coleccionDao.update(coleccion.copy(eliminado = false, fechaEliminacion = null))
    }

    suspend fun restaurarItem(item: Item) {
        itemDao.update(item.copy(eliminado = false, fechaEliminacion = null))
    }

    suspend fun eliminarColeccionDefinitivamente(coleccion: Coleccion) {
        coleccionDao.delete(coleccion)
    }

    suspend fun eliminarItemDefinitivamente(item: Item) {
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