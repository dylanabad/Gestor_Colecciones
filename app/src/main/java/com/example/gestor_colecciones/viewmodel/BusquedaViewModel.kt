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

/**
 * Estados posibles de la pantalla de búsqueda
 */
sealed class BusquedaState {

    /**
     * Sin búsqueda activa
     */
    object Idle : BusquedaState()

    /**
     * Buscando datos
     */
    object Loading : BusquedaState()

    /**
     * Resultado encontrado
     */
    data class Success(val resultado: BusquedaResultado) : BusquedaState()

    /**
     * No se encontraron resultados
     */
    object Empty : BusquedaState()
}

@OptIn(FlowPreview::class)
class BusquedaViewModel(
    private val repository: BusquedaRepository // Repositorio que ejecuta la búsqueda real
) : ViewModel() {

    /**
     * Query actual escrita por el usuario
     */
    private val _query = MutableStateFlow("")

    /**
     * Estado de la búsqueda (loading, success, etc.)
     */
    private val _state = MutableStateFlow<BusquedaState>(BusquedaState.Idle)

    /**
     * Estado expuesto a la UI
     */
    val state: StateFlow<BusquedaState> = _state

    init {
        viewModelScope.launch {

            // Observa cambios en la query con debounce para evitar búsquedas excesivas
            _query
                .debounce(300)
                .collectLatest { query ->

                    // Si está vacío, volvemos a estado idle
                    if (query.isBlank()) {
                        _state.value = BusquedaState.Idle
                        return@collectLatest
                    }

                    // Indicamos que está cargando
                    _state.value = BusquedaState.Loading

                    /**
                     * Ejecuta búsqueda en repositorio
                     */
                    val resultado = repository.buscar(query)

                    // Si no hay nada, estado vacío
                    _state.value =
                        if (resultado.colecciones.isEmpty() && resultado.items.isEmpty())
                            BusquedaState.Empty
                        else
                            BusquedaState.Success(resultado)
                }
        }
    }

    /**
     * Método llamado desde la UI para actualizar la búsqueda
     */
    fun buscar(query: String) {
        _query.value = query
    }
}