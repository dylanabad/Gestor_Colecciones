package com.example.gestor_colecciones.network

import java.util.Date
import java.util.Locale

// Objeto encargado de convertir fechas entre String y Date usando distintos formatos posibles
object DateMapper {

    // Lista de patrones de entrada que se intentarán para parsear un String a Date
    private val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX", // Fecha con milisegundos y zona horaria
        "yyyy-MM-dd'T'HH:mm:ss.SSS",  // Fecha con milisegundos sin zona horaria
        "yyyy-MM-dd'T'HH:mm:ssX",     // Fecha sin milisegundos con zona horaria
        "yyyy-MM-dd'T'HH:mm:ss"       // Fecha básica sin milisegundos ni zona horaria
    )

    // Patrón de salida fijo para formatear Date a String
    private const val OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    // Convierte un String en Date probando varios formatos posibles
    fun parse(value: String?): Date? {

        // Si el valor es nulo o vacío, devuelve null directamente
        if (value.isNullOrBlank()) return null

        // Recorre todos los patrones disponibles hasta encontrar uno válido
        for (pattern in parsePatterns) {
            try {
                // Crea un formatter con el patrón actual y locale US
                val formatter = java.text.SimpleDateFormat(pattern, Locale.US)

                // Intenta parsear la fecha con el formato actual
                return formatter.parse(value)

            } catch (_: Exception) {
                // Si falla el parseo, continúa con el siguiente patrón
            }
        }

        // Si ningún formato funciona, devuelve null
        return null
    }

    // Convierte un Date a String usando un formato fijo
    fun format(date: Date?): String? {

        // Si la fecha es nula, devuelve null
        if (date == null) return null

        // Crea un formatter con el patrón de salida definido
        val formatter = java.text.SimpleDateFormat(OUTPUT_PATTERN, Locale.US)

        // Devuelve la fecha formateada como String
        return formatter.format(date)
    }
}