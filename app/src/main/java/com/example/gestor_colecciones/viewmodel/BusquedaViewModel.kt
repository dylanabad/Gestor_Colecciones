package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.repository.BusquedaRepository
import com.example.gestor_colecciones.repository.BusquedaResultado
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class BusquedaState {
    object Idle : BusquedaState()
    object Loading : BusquedaState()
    data class Success(val resultado: BusquedaResultado) : BusquedaState()
    object Empty : BusquedaState()
}

@OptIn(FlowPreview::class)
class BusquedaViewModel(
    private val repository: BusquedaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow<BusquedaState>(BusquedaState.Idle)
    val state: StateFlow<BusquedaState> = _state

    init {
        viewModelScope.launch {
            _query.debounce(300).collectLatest { query ->
                if (query.isBlank()) {
                    _state.value = BusquedaState.Idle
                    return@collectLatest
                }
                _state.value = BusquedaState.Loading
                val resultado = repository.buscar(query)
                _state.value = if (resultado.colecciones.isEmpty() && resultado.items.isEmpty())
                    BusquedaState.Empty
                else
                    BusquedaState.Success(resultado)
            }
        }
    }

    fun buscar(query: String) {
        _query.value = query
    }
}