package com.example.gestor_colecciones.model

/*
 * ItemTagName.kt
 *
 * Data class simple que contiene el nombre de un tag asociado a un item.
 * Se utiliza cuando se necesita mostrar rápidamente los nombres de los tags
 * relacionados con un item (por ejemplo, en listas o en los detalles del item).
 *
 * Campos:
 * - itemId: identificador del item al que pertenece el tag
 * - nombre: nombre legible del tag (se muestra en la UI)
 *
 * Nota: Solo se añaden comentarios explicativos en español; no se modifica la lógica.
 */
data class ItemTagName(
    // ID del item asociado al tag
    val itemId: Int,
    // Nombre legible del tag
    val nombre: String
)

