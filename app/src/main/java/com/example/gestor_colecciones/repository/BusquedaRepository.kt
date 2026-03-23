package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item

data class BusquedaResultado(
    val colecciones: List<Coleccion>,
    val items: List<Item>
)

class BusquedaRepository(
    private val coleccionDao: ColeccionDao,
    private val itemDao: ItemDao
) {
    suspend fun buscar(query: String): BusquedaResultado {
        if (query.isBlank()) return BusquedaResultado(emptyList(), emptyList())
        return BusquedaResultado(
            colecciones = coleccionDao.searchColecciones(query),
            items = itemDao.searchItems(query)
        )
    }
}