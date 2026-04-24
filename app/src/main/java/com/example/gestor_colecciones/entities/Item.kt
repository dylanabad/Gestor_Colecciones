package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * Entidad Room de un item coleccionable con metadatos, estado y referencias.
 */
@Entity(
    tableName = "Item",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Coleccion::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoriaId"), Index("collectionId")]
)
data class Item(

    // Identificador único del item
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Título o nombre del item
    val titulo: String,

    // Relación con la categoría
    val categoriaId: Int,

    // Relación con la colección
    val collectionId: Int,

    // Fecha en la que se adquirió el item
    val fechaAdquisicion: Date,

    // Valor monetario del item
    val valor: Double,

    // Ruta de imagen asociada al item (opcional)
    val imagenPath: String?,

    // Estado del item (ej: nuevo, usado, etc.)
    val estado: String,

    // Descripción opcional del item
    val descripcion: String?,

    // Valoración del item (0–5, por ejemplo)
    val calificacion: Float,

    // Indica si el item está eliminado (papelera)
    val eliminado: Boolean = false,

    // Fecha en la que fue eliminado (si aplica)
    val fechaEliminacion: Date? = null,

    // Indica si el item está prestado actualmente
    val prestado: Boolean = false,

    // Marca si el item es favorito
    val favorito: Boolean = false
)