package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.CategoriaRepository

/**
 * Factory encargada de crear instancias de ItemViewModel con sus dependencias
 */
class ItemViewModelFactory(
    private val itemRepository: ItemRepository,
    private val categoriaRepository: CategoriaRepository? = null, // opcional
    private val historyRepository: ItemHistoryRepository? = null  // opcional
) : ViewModelProvider.Factory {

    /**
     * Crea el ViewModel solicitado por el sistema
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica si el ViewModel solicitado es ItemViewModel
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(
                itemRepository,
                categoriaRepository,
                historyRepository
            ) as T
        }

        // Si no coincide, lanza error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}