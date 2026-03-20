package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.repository.ColeccionExportData
import com.example.gestor_colecciones.repository.ExportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StatsState {
    object Loading : StatsState()
    data class Success(val data: List<ColeccionExportData>) : StatsState()
    data class Error(val message: String) : StatsState()
}

class StatsViewModel(
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StatsState>(StatsState.Loading)
    val state: StateFlow<StatsState> = _state

    init {
        loadStats()
    }

    fun loadStats() = viewModelScope.launch {
        _state.value = StatsState.Loading
        runCatching {
            exportRepository.getDataForExport()
        }.onSuccess {
            _state.value = StatsState.Success(it)
        }.onFailure {
            _state.value = StatsState.Error(it.message ?: "Error al cargar estadísticas")
        }
    }
}