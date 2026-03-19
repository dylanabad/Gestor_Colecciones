package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item

data class ColeccionExportData(
    val coleccion: Coleccion,
    val items: List<Item>
)

class ExportRepository(
    private val coleccionRepository: ColeccionRepository,
    private val itemRepository: ItemRepository
) {
    suspend fun getDataForExport(ids: List<Int>? = null): List<ColeccionExportData> {
        val colecciones = coleccionRepository.getAllOnce()
        val filtradas = if (ids != null) colecciones.filter { it.id in ids } else colecciones
        return filtradas.map { coleccion ->
            ColeccionExportData(
                coleccion = coleccion,
                items = itemRepository.getItemsByCollectionOnce(coleccion.id)
            )
        }
    }
}