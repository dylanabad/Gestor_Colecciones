package com.example.gestor_colecciones.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var instance: AppDataBase? = null  // Usar el nombre correcto de la clase

    fun getDatabase(context: Context): AppDataBase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java, // Nombre correcto de la clase
                "gestor_colecciones_db" // Nombre de la base de datos
            ).build()
            instance = db
            db
        }
    }
}