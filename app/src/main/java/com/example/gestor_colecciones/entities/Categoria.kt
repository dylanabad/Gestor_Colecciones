package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una categoria personal del usuario.
 */
@Entity(tableName = "Categoria")
data class Categoria(

    // Clave primaria autogenerada
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre de la categoría
    val nombre: String
)