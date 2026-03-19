package com.example.gestor_colecciones.model

data class ColeccionColorOption(
    val name: String,
    val color: Int
)

object ColeccionColors {
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

