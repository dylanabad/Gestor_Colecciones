package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.PapeleraRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PapeleraViewModel(
    private val repository: PapeleraRepository
) : ViewModel() {

    val coleccionesEliminadas: StateFlow<List<Coleccion>> = repository.coleccionesEliminadas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemsEliminados: StateFlow<List<Item>> = repository.itemsEliminados
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.limpiarElementosAntiguos() }
    }

    fun restaurarColeccion(coleccion: Coleccion) = viewModelScope.launch {
        repository.restaurarColeccion(coleccion)
    }

    fun restaurarItem(item: Item) = viewModelScope.launch {
        repository.restaurarItem(item)
    }

    fun eliminarColeccionDefinitivamente(coleccion: Coleccion) = viewModelScope.launch {
        repository.eliminarColeccionDefinitivamente(coleccion)
    }

    fun eliminarItemDefinitivamente(item: Item) = viewModelScope.launch {
        repository.eliminarItemDefinitivamente(item)
    }
}