package com.example.gestor_colecciones.model

/*
 * ColeccionColors.kt
 *
 * Definición de una paleta de colores disponible para asignar a las colecciones.
 * Incluye una pequeña data class que representa una opción (nombre + color) y
 * un objeto singleton `ColeccionColors` que expone la paleta `PALETTE`.
 *
 * Los colores están definidos como enteros ARGB y se convierten con toInt().
 *
 */

// Representa una opción de color para una colección (nombre legible y valor entero de color)
data class ColeccionColorOption(
    val name: String,
    val color: Int
)

// Objeto que contiene la paleta de colores disponibles en la app
object ColeccionColors {
    // Lista ordenada de opciones que pueden mostrarse en un selector de color
    val PALETTE: List<ColeccionColorOption> = listOf(
        ColeccionColorOption("Rojo", 0xFFE53935.toInt()),
        ColeccionColorOption("Rosa", 0xFFD81B60.toInt()),
        ColeccionColorOption("Morado", 0xFF8E24AA.toInt()),
        ColeccionColorOption("Índigo", 0xFF3949AB.toInt()),
        ColeccionColorOption("Azul", 0xFF1E88E5.toInt()),
        ColeccionColorOption("Turquesa", 0xFF00897B.toInt()),
        ColeccionColorOption("Verde", 0xFF43A047.toInt()),
        ColeccionColorOption("Lima", 0xFFC0CA33.toInt()),
        ColeccionColorOption("Ámbar", 0xFFFFB300.toInt()),
        ColeccionColorOption("Naranja", 0xFFFB8C00.toInt())
    )
}

