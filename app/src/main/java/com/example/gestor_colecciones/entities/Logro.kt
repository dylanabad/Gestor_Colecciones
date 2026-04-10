package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Entidad que representa un logro desbloqueable dentro de la aplicación
@Entity(tableName = "Logro")
data class Logro(

    // Clave única del logro (identificador manual)
    @PrimaryKey
    val key: String,

    // Indica si el logro ha sido desbloqueado
    val desbloqueado: Boolean = false,

    // Fecha en la que se desbloqueó el logro (si aplica)
    val fechaDesbloqueo: Date? = null
)