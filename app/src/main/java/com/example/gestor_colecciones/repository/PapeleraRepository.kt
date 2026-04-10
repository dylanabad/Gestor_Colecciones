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

// Repositorio encargado de gestionar la papelera (soft delete, restauración y borrado definitivo)
class PapeleraRepository(
    private val coleccionDao: ColeccionDao, // Acceso a colecciones en BD local
    private val itemDao: ItemDao,           // Acceso a items en BD local
    private val api: ApiService             // Acceso a API remota
) {

    // Flujo con colecciones marcadas como eliminadas (papelera)
    val coleccionesEliminadas: Flow<List<Coleccion>> =
        coleccionDao.getColeccionesEliminadas()

    // Flujo con items marcados como eliminados (papelera)
    val itemsEliminados: Flow<List<Item>> =
        itemDao.getItemsEliminados()

    // Mueve una colección a la papelera (soft delete)
    suspend fun moverColeccionAPapelera(coleccion: Coleccion) {

        // Notifica al backend eliminación lógica
        api.deleteColeccion(coleccion.id.toLong())

        // Marca la colección como eliminada en local
        coleccionDao.update(
            coleccion.copy(
                eliminado = true,
                fechaEliminacion = Date()
            )
        )
    }

    // Mueve un item a la papelera (soft delete)
    suspend fun moverItemAPapelera(item: Item) {

        // Envía versión eliminada al backend
        api.saveItem(
            item.collectionId.toLong(),
            item.categoriaId.takeIf { it > 0 }?.toLong(),
            item.copy(
                eliminado = true,
                fechaEliminacion = Date()
            ).toDto()
        )

        // Marca el item como eliminado en local
        itemDao.update(
            item.copy(
                eliminado = true,
                fechaEliminacion = Date()
            )
        )
    }

    // Restaura una colección desde la papelera
    suspend fun restaurarColeccion(coleccion: Coleccion) {

        // Actualiza en backend quitando el estado de eliminado
        api.saveColeccion(
            coleccion.copy(
                eliminado = false,
                fechaEliminacion = null
            ).toDto()
        )

        // Actualiza en local
        coleccionDao.update(
            coleccion.copy(
                eliminado = false,
                fechaEliminacion = null
            )
        )
    }

    // Restaura un item desde la papelera
    suspend fun restaurarItem(item: Item) {

        // Actualiza en backend
        api.saveItem(
            item.collectionId.toLong(),
            item.categoriaId.takeIf { it > 0 }?.toLong(),
            item.copy(
                eliminado = false,
                fechaEliminacion = null
            ).toDto()
        )

        // Actualiza en local
        itemDao.update(
            item.copy(
                eliminado = false,
                fechaEliminacion = null
            )
        )
    }

    // Elimina una colección de forma permanente
    suspend fun eliminarColeccionDefinitivamente(coleccion: Coleccion) {

        // Eliminación física en backend
        api.deleteColeccionHard(coleccion.id.toLong())

        // Eliminación en base de datos local
        coleccionDao.delete(coleccion)
    }

    // Elimina un item de forma permanente
    suspend fun eliminarItemDefinitivamente(item: Item) {

        // Eliminación física en backend
        api.deleteItemHard(item.id.toLong())

        // Eliminación en base de datos local
        itemDao.delete(item)
    }

    // Limpia elementos antiguos de la papelera (más de 30 días)
    suspend fun limpiarElementosAntiguos() {

        // Calcula timestamp de hace 30 días
        val hace30Dias = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis

        // Limpia colecciones antiguas
        coleccionDao.limpiarColeccionesAntiguas(hace30Dias)

        // Limpia items antiguos
        itemDao.limpiarItemsAntiguos(hace30Dias)
    }
}