package com.example.gestor_colecciones.repository

import android.content.Context
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.toDto
import com.example.gestor_colecciones.network.dto.toEntity
import com.example.gestor_colecciones.widget.ColeccionesWidgetProvider
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Repositorio encargado de gestionar colecciones entre base de datos local y API remota
class ColeccionRepository(
    private val context: Context,
    private val coleccionDao: ColeccionDao, // Acceso a la BD local
    private val api: ApiService             // Acceso a la API remota
) {

    // Flujo reactivo con todas las colecciones almacenadas localmente
    val allColecciones: Flow<List<Coleccion>> = coleccionDao.getAllColecciones()

    // Inserta una colección: primero en la API y luego en la base de datos local
    suspend fun insert(coleccion: Coleccion): Long {

        // Guarda la colección en el servidor y recibe la versión actualizada
        val saved = api.saveColeccion(coleccion.toDto())

        // Inserta la versión sincronizada en la base de datos local
        return coleccionDao.insert(saved.toEntity()).also {
            ColeccionesWidgetProvider.refreshAllWidgets(context)
        }
    }

    // Actualiza una colección en API y base de datos local
    suspend fun update(coleccion: Coleccion) {

        // Guarda cambios en la API
        val saved = api.saveColeccion(coleccion.toDto())

        // Actualiza en local la versión devuelta por el servidor
        // Usamos update() en lugar de insert() para evitar que OnConflictStrategy.REPLACE
        // dispare un DELETE que borre los items en cascada.
        coleccionDao.update(saved.toEntity())
        ColeccionesWidgetProvider.refreshAllWidgets(context)
    }

    // Elimina una colección en la API y marca como eliminada en local
    suspend fun delete(coleccion: Coleccion) {

        // Elimina en la API remota
        api.deleteColeccion(coleccion.id.toLong())

        // Marca la colección como eliminada localmente (soft delete)
        coleccionDao.update(
            coleccion.copy(
                eliminado = true,
                fechaEliminacion = Date()
            )
        )
        ColeccionesWidgetProvider.refreshAllWidgets(context)
    }

    // Obtiene una colección por ID desde la base de datos local
    suspend fun getById(id: Int): Coleccion? =
        coleccionDao.getColeccionById(id)

    // Obtiene todas las colecciones una sola vez (sin Flow)
    suspend fun getAllOnce(): List<Coleccion> =
        coleccionDao.getAllColeccionesOnce()
}
