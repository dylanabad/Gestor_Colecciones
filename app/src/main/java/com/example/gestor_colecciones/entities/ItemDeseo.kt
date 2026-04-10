package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Entidad que representa un item en la lista de deseos
@Entity(tableName = "ItemDeseo")
data class ItemDeseo(

    // Identificador único autogenerado
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Título del item deseado
    val titulo: String,

    // Descripción opcional del item
    val descripcion: String? = null,

    // Precio objetivo estimado del item
    val precioObjetivo: Double = 0.0,

    // Prioridad del deseo (1 = alta, 2 = media, 3 = baja)
    val prioridad: Int = 2,

    // Enlace externo relacionado con el item (opcional)
    val enlace: String? = null,

    // Indica si el item ya ha sido conseguido
    val conseguido: Boolean = false,

    // Fecha de creación del registro
    val fechaCreacion: Date = Date(),

    // Fecha en la que se consiguió el item (si aplica)
    val fechaConseguido: Date? = null
)