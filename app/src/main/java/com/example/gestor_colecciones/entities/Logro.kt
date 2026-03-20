package com.example.gestor_colecciones.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Logro")
data class Logro(
    @PrimaryKey
    val key: String,
    val desbloqueado: Boolean = false,
    val fechaDesbloqueo: Date? = null
)