package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que modela una persona relacionada con prestamos o contactos.
 */
@Entity(tableName = "Persona")
data class Persona(

    // Identificador único de la persona
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre de la persona
    val nombre: String,

    // Información de contacto (teléfono, email, etc.)
    val contacto: String?,

    // ID del usuario propietario de este registro
    val usuarioId: Int = 0,

    // Referencia opcional a un usuario registrado en el sistema
    val usuarioRefId: Int? = null
)