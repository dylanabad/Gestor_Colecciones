package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.repository.LogroRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogroViewModel(
    private val logroRepository: LogroRepository,
    private val logroManager: LogroManager
) : ViewModel() {

    val logros: StateFlow<List<Logro>> = logroRepository.allLogros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Evento puntual para mostrar notificación de logro desbloqueado
    private val _nuevoLogro = MutableSharedFlow<String>()
    val nuevoLogro: SharedFlow<String> = _nuevoLogro

    fun checkLogros() = viewModelScope.launch {
        logroManager.checkAll().forEach { key ->
            _nuevoLogro.emit(key)
        }
    }
}