package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Movimiento
import com.example.gestor_colecciones.repository.MovimientoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel encargado de gestionar los movimientos de items
class MovimientoViewModel(
    private val repository: MovimientoRepository
) : ViewModel() {

    // Lista reactiva de todos los movimientos
    val movimientos: StateFlow<List<Movimiento>> =
        repository.allMovimientos.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Inserta un nuevo movimiento en la base de datos
    fun insert(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.insert(movimiento)
        }
    }

    // Obtiene movimientos filtrados por item
    fun getMovimientosByItem(itemId: Int) =
        repository.getMovimientosByItem(itemId)

    // Obtiene movimientos filtrados por persona
    fun getMovimientosByPersona(personaId: Int) =
        repository.getMovimientosByPersona(personaId)
}