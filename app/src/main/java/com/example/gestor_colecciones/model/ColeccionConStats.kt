package com.example.gestor_colecciones.model

import com.example.gestor_colecciones.entities.Coleccion

/*
 * ColeccionConStats.kt
 *
 * Pequeña data class que agrupa una `Coleccion` con estadísticas derivadas:
 * - `totalItems`: número de items dentro de la colección
 * - `valorTotal`: suma de los valores de esos items
 *
 * Esta clase se usa como DTO/portador de datos cuando se calculan y muestran
 * estadísticas por colección en la UI.
 */
data class ColeccionConStats(
    // Entidad de la colección original
    val coleccion: Coleccion,
    // Número total de items que pertenecen a la colección
    val totalItems: Int,
    // Suma total del campo `valor` de todos los items de la colección
    val valorTotal: Double
)