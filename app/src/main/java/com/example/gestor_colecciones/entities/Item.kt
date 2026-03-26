package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

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
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val categoriaId: Int,
    val collectionId: Int,
    val fechaAdquisicion: Date,
    val valor: Double,
    val imagenPath: String?,
    val estado: String,
    val descripcion: String?,
    val calificacion: Float,
    val eliminado: Boolean = false,
    val fechaEliminacion: Date? = null,
    val prestado: Boolean = false       // ← añadido
)