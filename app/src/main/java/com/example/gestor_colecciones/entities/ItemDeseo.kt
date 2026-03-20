package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "ItemDeseo")
data class ItemDeseo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val descripcion: String? = null,
    val precioObjetivo: Double = 0.0,
    val prioridad: Int = 2,        // 1=Alta, 2=Media, 3=Baja
    val enlace: String? = null,    // URL donde encontrar el item
    val conseguido: Boolean = false,
    val fechaCreacion: Date = Date(),
    val fechaConseguido: Date? = null
)