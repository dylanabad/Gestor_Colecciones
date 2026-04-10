package com.example.gestor_colecciones.model

/*
 * ItemFilterSortState.kt
 *
 * Contiene tipos ligeros usados para representar el estado de filtrado y ordenación
 * en las pantallas de lista de items. Se usa para pasar el estado entre el BottomSheet
 * de filtros/orden y el fragmento que muestra la lista.
 */

// Campos posibles por los que se puede ordenar la lista de items
enum class ItemSortField {
    // Ordenar por nombre del item
    NAME,
    // Ordenar por valor numérico del item
    VALUE,
    // Ordenar por fecha de adquisición
    DATE
}

// Data class que agrupa los criterios de filtrado y ordenación seleccionados por el usuario
data class ItemFilterSortState(
    // Filtrar por categoría (id de la categoría). Null = cualquiera
    val categoriaId: Int? = null,
    // Filtrar por tag (id del tag). Null = cualquiera
    val tagId: Int? = null,
    // Filtrar por estado textual (por ejemplo "Nuevo", "Usado"). Null = cualquiera
    val estado: String? = null,
    // Calificación mínima a filtrar (0f = cualquiera)
    val minCalificacion: Float = 0f,
    // Campo por el que ordenar (por defecto por fecha)
    val sortField: ItemSortField = ItemSortField.DATE,
    // Dirección de la ordenación: true = ascendente, false = descendente
    val ascending: Boolean = false
)
