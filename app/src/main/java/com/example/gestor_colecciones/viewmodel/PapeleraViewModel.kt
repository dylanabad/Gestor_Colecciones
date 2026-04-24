package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.repository.PapeleraRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar la papelera (elementos eliminados)
 */
class PapeleraViewModel(
    private val repository: PapeleraRepository
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val uiEvents = _uiEvents.asSharedFlow()

    private val _vaciando = MutableStateFlow(false)
    val vaciando = _vaciando.asStateFlow()

    /**
     * Colecciones eliminadas de forma reactiva
     */
    val coleccionesEliminadas: StateFlow<List<Coleccion>> =
        repository.coleccionesEliminadas.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Items eliminados de forma reactiva
     */
    val itemsEliminados: StateFlow<List<Item>> =
        repository.itemsEliminados.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Deseos eliminados de forma reactiva
     */
    val deseosEliminados: StateFlow<List<ItemDeseo>> =
        repository.deseosEliminados.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Al iniciar el ViewModel se limpian elementos antiguos automáticamente
    init {
        viewModelScope.launch {
            repository.limpiarElementosAntiguos()
        }
    }

    /**
     * Restaura una colección eliminada
     */
    fun restaurarColeccion(coleccion: Coleccion) =
        viewModelScope.launch {
            repository.restaurarColeccion(coleccion)
        }

    /**
     * Restaura un item eliminado
     */
    fun restaurarItem(item: Item) =
        viewModelScope.launch {
            repository.restaurarItem(item)
        }

    /**
     * Elimina definitivamente una colección
     */
    fun eliminarColeccionDefinitivamente(coleccion: Coleccion) =
        viewModelScope.launch {
            repository.eliminarColeccionDefinitivamente(coleccion)
        }

    /**
     * Elimina definitivamente un item
     */
    fun eliminarItemDefinitivamente(item: Item) =
        viewModelScope.launch {
            repository.eliminarItemDefinitivamente(item)
        }

    fun restaurarDeseo(deseo: ItemDeseo) =
        viewModelScope.launch {
            repository.restaurarDeseo(deseo)
        }

    fun eliminarDeseoDefinitivamente(deseo: ItemDeseo) =
        viewModelScope.launch {
            repository.eliminarDeseoDefinitivamente(deseo)
        }

    fun vaciarPestania(tabIndex: Int) = viewModelScope.launch {
        if (_vaciando.value) return@launch

        _vaciando.value = true
        try {
            when (tabIndex) {
                0 -> {
                    val lista = coleccionesEliminadas.value
                    if (lista.isEmpty()) {
                        _uiEvents.tryEmit("No hay colecciones en la papelera")
                    } else {
                        repository.vaciarColeccionesEliminadas(lista)
                        _uiEvents.tryEmit("Colecciones eliminadas definitivamente")
                    }
                }

                1 -> {
                    val lista = itemsEliminados.value
                    if (lista.isEmpty()) {
                        _uiEvents.tryEmit("No hay items en la papelera")
                    } else {
                        repository.vaciarItemsEliminados(lista)
                        _uiEvents.tryEmit("Items eliminados definitivamente")
                    }
                }

                else -> {
                    val lista = deseosEliminados.value
                    if (lista.isEmpty()) {
                        _uiEvents.tryEmit("No hay deseos en la papelera")
                    } else {
                        repository.vaciarDeseosEliminados(lista)
                        _uiEvents.tryEmit("Deseos eliminados definitivamente")
                    }
                }
            }
        } catch (e: Exception) {
            _uiEvents.tryEmit("Error al vaciar la papelera: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            _vaciando.value = false
        }
    }
}
