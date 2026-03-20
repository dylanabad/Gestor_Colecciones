package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.repository.ItemDeseoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeseoViewModel(private val repository: ItemDeseoRepository) : ViewModel() {

    val all: StateFlow<List<ItemDeseo>> = repository.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countPendientes: StateFlow<Int> = repository.countPendientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun insert(item: ItemDeseo) = viewModelScope.launch { repository.insert(item) }
    fun update(item: ItemDeseo) = viewModelScope.launch { repository.update(item) }
    fun delete(item: ItemDeseo) = viewModelScope.launch { repository.delete(item) }
    fun marcarConseguido(item: ItemDeseo) = viewModelScope.launch {
        repository.marcarConseguido(item)
    }
}