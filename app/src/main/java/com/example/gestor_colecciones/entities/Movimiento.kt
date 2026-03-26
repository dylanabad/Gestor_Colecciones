package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "Movimiento",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Persona::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("itemId"), Index("personaId")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemId: Int,
    val tipo: String,                           // PRESTAMO | DEVOLUCION
    val personaId: Int?,
    val fechaHora: Date,
    val estado: String? = null,                 // ← ACTIVO | DEVUELTO
    val fechaDevolucionPrevista: Date? = null,  // ←
    val fechaDevolucionReal: Date? = null,      // ←
    val notas: String? = null                   // ←
)