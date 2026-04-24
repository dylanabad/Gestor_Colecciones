package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad Room que representa una coleccion principal dentro del inventario.
 */
@Entity(tableName = "Coleccion")
data class Coleccion(

    // Identificador único autogenerado
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre de la colección
    val nombre: String,

    // Descripción opcional de la colección
    val descripcion: String?,

    // Fecha en la que se creó la colección
    val fechaCreacion: Date,

    // Ruta de la imagen asociada a la colección (opcional)
    val imagenPath: String? = null,

    // Color asociado a la colección (para UI)
    val color: Int = 0,

    // Número de items que contiene la colección (dato derivado/cacheado)
    var itemsCount: Int = 0,

    // Valor total de los items de la colección (dato derivado/cacheado)
    var totalValue: Double = 0.0,

    // Indica si la colección está eliminada (papelera)
    val eliminado: Boolean = false,

    // Fecha en la que fue eliminada (si aplica)
    val fechaEliminacion: Date? = null
)