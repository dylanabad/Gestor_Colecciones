package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * Entidad Room que representa un movimiento o evento dentro del flujo de prestamos.
 */
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

    // Identificador único del movimiento
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID del item involucrado en el movimiento
    val itemId: Int,

    // Tipo de movimiento (ej: PRESTAMO, DEVOLUCION)
    val tipo: String,

    // Persona asociada al movimiento (puede ser null)
    val personaId: Int?,

    // Fecha y hora del movimiento
    val fechaHora: Date,

    // Estado del movimiento (ACTIVO, DEVUELTO, etc.)
    val estado: String? = null,

    // Fecha prevista de devolución
    val fechaDevolucionPrevista: Date? = null,

    // Fecha real de devolución
    val fechaDevolucionReal: Date? = null,

    // Notas adicionales del movimiento
    val notas: String? = null
)