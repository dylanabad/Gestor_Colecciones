package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Movimiento
import com.example.gestor_colecciones.repository.MovimientoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovimientoViewModel(private val repository: MovimientoRepository) : ViewModel() {

    val movimientos: StateFlow<List<Movimiento>> =
        repository.allMovimientos.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.insert(movimiento)
        }
    }

    fun getMovimientosByItem(itemId: Int) =
        repository.getMovimientosByItem(itemId)

    fun getMovimientosByPersona(personaId: Int) =
        repository.getMovimientosByPersona(personaId)
}