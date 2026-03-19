package com.example.gestor_colecciones.model

enum class ItemSortField {
    NAME,
    VALUE,
    DATE
}

data class ItemFilterSortState(
    val categoriaId: Int? = null,
    val estado: String? = null,
    val minCalificacion: Float = 0f,
    val sortField: ItemSortField = ItemSortField.DATE,
    val ascending: Boolean = false
)

