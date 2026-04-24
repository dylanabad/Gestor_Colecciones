package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que registra eventos historicos asociados a items.
 */
@Entity(
    tableName = "item_history",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class ItemHistory(

    // Identificador único del registro de historial
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID del item al que pertenece este registro de historial
    val itemId: Int,

    // Tipo de evento registrado (ej: creación, edición, préstamo, etc.)
    val tipo: String,

    // Fecha en la que ocurrió el evento
    val fecha: Date,

    // Descripción opcional del cambio realizado
    val descripcion: String? = null
)