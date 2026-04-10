package com.example.gestor_colecciones.converter

import androidx.room.TypeConverter
import java.util.Date

// Convertidor usado por Room para poder guardar objetos Date en la base de datos
// (Room no soporta Date directamente, así que se convierte a Long)
class DateConverter {

    // Convierte un timestamp (Long) a un objeto Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // Convierte un objeto Date a timestamp (Long)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}