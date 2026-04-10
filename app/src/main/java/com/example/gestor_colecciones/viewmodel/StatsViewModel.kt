package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.repository.ColeccionExportData
import com.example.gestor_colecciones.repository.ExportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estados posibles para la pantalla de estadísticas
sealed class StatsState {
    object Loading : StatsState()
    data class Success(val data: List<ColeccionExportData>) : StatsState()
    data class Error(val message: String) : StatsState()
}

// ViewModel encargado de cargar y exponer estadísticas exportadas
class StatsViewModel(
    private val exportRepository: ExportRepository
) : ViewModel() {

    // Estado reactivo de la pantalla (loading / success / error)
    private val _state = MutableStateFlow<StatsState>(StatsState.Loading)
    val state: StateFlow<StatsState> = _state

    // Carga automática al crear el ViewModel
    init {
        loadStats()
    }

    // Carga estadísticas desde el repositorio
    fun loadStats() = viewModelScope.launch {
        _state.value = StatsState.Loading

        runCatching {
            exportRepository.getDataForExport()
        }.onSuccess { data ->
            _state.value = StatsState.Success(data)
        }.onFailure { error ->
            _state.value = StatsState.Error(
                error.message ?: "Error al cargar estadísticas"
            )
        }
    }
}