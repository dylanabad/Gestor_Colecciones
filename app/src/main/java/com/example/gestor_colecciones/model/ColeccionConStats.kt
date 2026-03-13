package com.example.gestor_colecciones.model
import com.example.gestor_colecciones.entities.Coleccion

data class ColeccionConStats(
    val coleccion: Coleccion,
    val totalItems: Int,
    val valorTotal: Double
)