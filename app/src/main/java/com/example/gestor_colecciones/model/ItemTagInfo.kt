package com.example.gestor_colecciones.model

/*
 * ItemTagInfo.kt
 *
 * Data class ligera que representa la relación entre un item y un tag junto con
 * el nombre del tag. Se usa para consultas que devuelven información de tags
 * asociados a items (por ejemplo, para mostrar nombres de tags en las listas).
 *
 * Campos:
 * - itemId: id del item al que pertenece el tag
 * - tagId: id del tag
 * - nombre: nombre legible del tag
 */
data class ItemTagInfo(
    // Identificador del item asociado
    val itemId: Int,
    // Identificador del tag asociado
    val tagId: Int,
    // Nombre legible del tag (se usa en la UI)
    val nombre: String
)

