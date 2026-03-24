package com.example.gestor_colecciones.network

import java.util.Date
import java.util.Locale

object DateMapper {
    private val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    private const val OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    fun parse(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        for (pattern in parsePatterns) {
            try {
                val formatter = java.text.SimpleDateFormat(pattern, Locale.US)
                return formatter.parse(value)
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    fun format(date: Date?): String? {
        if (date == null) return null
        val formatter = java.text.SimpleDateFormat(OUTPUT_PATTERN, Locale.US)
        return formatter.format(date)
    }
}
