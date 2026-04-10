package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item

// Data class que agrupa los resultados de una búsqueda
data class BusquedaResultado(
    val colecciones: List<Coleccion>, // Lista de colecciones que coinciden con la búsqueda
    val items: List<Item>             // Lista de items que coinciden con la búsqueda
)

// Repositorio encargado de realizar búsquedas en colecciones e items
class BusquedaRepository(
    private val coleccionDao: ColeccionDao, // Acceso a datos de colecciones
    private val itemDao: ItemDao            // Acceso a datos de items
) {

    // Realiza una búsqueda en ambas fuentes de datos
    suspend fun buscar(query: String): BusquedaResultado {

        // Si la consulta está vacía, devuelve resultados vacíos
        if (query.isBlank()) return BusquedaResultado(emptyList(), emptyList())

        // Devuelve los resultados obtenidos de ambos DAOs
        return BusquedaResultado(
            colecciones = coleccionDao.searchColecciones(query),
            items = itemDao.searchItems(query)
        )
    }
}