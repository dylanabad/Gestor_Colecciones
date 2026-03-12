package com.example.gestor_colecciones.database

import android.content.Context
import androidx.room.Room
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.dao.CategoriaDao

object DatabaseProvider {

    private var instance: AppDataBase? = null

    fun getDatabase(context: Context): AppDataBase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "gestor_colecciones_db"
            ).build()
            instance = db
            db
        }
    }

    // Funciones helper para acceder a los DAOs
    fun getColeccionDao(context: Context): ColeccionDao = getDatabase(context).coleccionDao()
    fun getItemDao(context: Context): ItemDao = getDatabase(context).itemDao()
    fun getCategoriaDao(context: Context): CategoriaDao = getDatabase(context).categoriaDao()
}