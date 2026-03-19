package com.example.gestor_colecciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.CategoriaRepository

class ItemViewModelFactory(
    private val itemRepository: ItemRepository,
    private val categoriaRepository: CategoriaRepository? = null,
    private val historyRepository: ItemHistoryRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(itemRepository, categoriaRepository, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}