package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item

/**
 * Data class que agrupa una colección junto con sus items para exportación
 */
data class ColeccionExportData(
    val coleccion: Coleccion, // Datos de la colección
    val items: List<Item>     // Items pertenecientes a la colección
)

/**
 * Repositorio encargado de preparar datos para exportación
 */
class ExportRepository(
    private val coleccionRepository: ColeccionRepository, // Acceso a colecciones
    private val itemRepository: ItemRepository            // Acceso a items
) {

    /**
     * Obtiene datos listos para exportar, opcionalmente filtrados por IDs de colección
     */
    suspend fun getDataForExport(ids: List<Int>? = null): List<ColeccionExportData> {

        /**
         * Obtiene todas las colecciones desde el repositorio
         */
        val colecciones = coleccionRepository.getAllOnce()

        /**
         * Si se proporcionan IDs, filtra solo esas colecciones
         */
        val filtradas = if (ids != null)
            colecciones.filter { it.id in ids }
        else
            colecciones

        // Construye la estructura de exportación combinando colección + sus items
        return filtradas.map { coleccion ->

            ColeccionExportData(
                coleccion = coleccion,
                items = itemRepository.getItemsByCollectionOnce(coleccion.id)
            )
        }
    }
}