package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room de una etiqueta creada por el usuario.
 */
@Entity(tableName = "Tag")
data class Tag(

    // Identificador único de la etiqueta
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre de la etiqueta
    val nombre: String
)