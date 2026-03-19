package com.example.gestor_colecciones.entities
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Coleccion")
data class Coleccion(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,

    val descripcion: String?,

    val fechaCreacion: Date,

    val imagenPath: String? = null,

    val color: Int = 0,

    var itemsCount: Int = 0,
    var totalValue: Double = 0.0


)
