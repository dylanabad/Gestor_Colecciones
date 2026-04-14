package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.repository.PapeleraRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel encargado de gestionar la papelera (elementos eliminados)
class PapeleraViewModel(
    private val repository: PapeleraRepository
) : ViewModel() {

    // Colecciones eliminadas de forma reactiva
    val coleccionesEliminadas: StateFlow<List<Coleccion>> =
        repository.coleccionesEliminadas.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Items eliminados de forma reactiva
    val itemsEliminados: StateFlow<List<Item>> =
        repository.itemsEliminados.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Deseos eliminados de forma reactiva
    val deseosEliminados: StateFlow<List<ItemDeseo>> =
        repository.deseosEliminados.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Al iniciar el ViewModel se limpian elementos antiguos automáticamente
    init {
        viewModelScope.launch {
            repository.limpiarElementosAntiguos()
        }
    }

    // Restaura una colección eliminada
    fun restaurarColeccion(coleccion: Coleccion) =
        viewModelScope.launch {
            repository.restaurarColeccion(coleccion)
        }

    // Restaura un item eliminado
    fun restaurarItem(item: Item) =
        viewModelScope.launch {
            repository.restaurarItem(item)
        }

    // Elimina definitivamente una colección
    fun eliminarColeccionDefinitivamente(coleccion: Coleccion) =
        viewModelScope.launch {
            repository.eliminarColeccionDefinitivamente(coleccion)
        }

    // Elimina definitivamente un item
    fun eliminarItemDefinitivamente(item: Item) =
        viewModelScope.launch {
            repository.eliminarItemDefinitivamente(item)
        }

    fun restaurarDeseo(deseo: ItemDeseo) =
        viewModelScope.launch {
            repository.restaurarDeseo(deseo)
        }

    fun eliminarDeseoDefinitivamente(deseo: ItemDeseo) =
        viewModelScope.launch {
            repository.eliminarDeseoDefinitivamente(deseo)
        }
}
