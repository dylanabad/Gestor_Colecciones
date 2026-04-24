package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.repository.ItemDeseoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar la lista de deseos
 */
class DeseoViewModel(
    private val repository: ItemDeseoRepository
) : ViewModel() {

    /**
     * Lista reactiva de deseos
     */
    val all: StateFlow<List<ItemDeseo>> =
        repository.all.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Contador reactivo de deseos pendientes
     */
    val countPendientes: StateFlow<Int> =
        repository.countPendientes.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    /**
     * Inserta un nuevo deseo
     */
    fun insert(item: ItemDeseo) =
        viewModelScope.launch { repository.insert(item) }

    /**
     * Actualiza un deseo
     */
    fun update(item: ItemDeseo) =
        viewModelScope.launch { repository.update(item) }

    /**
     * Elimina un deseo
     */
    fun delete(item: ItemDeseo) =
        viewModelScope.launch { repository.delete(item) }

    /**
     * Marca un deseo como conseguido
     */
    fun marcarConseguido(item: ItemDeseo) =
        viewModelScope.launch {
            repository.marcarConseguido(item)
        }
}