package com.example.gestor_colecciones.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Persona
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.Movimiento
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.entities.ItemTag
import com.example.gestor_colecciones.entities.ItemHistory
import com.example.gestor_colecciones.converter.DateConverter
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.dao.PersonaDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.dao.MovimientoDao
import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.dao.ItemHistoryDao
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.dao.LogroDao
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.dao.ItemDeseoDao
@Database(
    entities = [
        Coleccion::class,
        Categoria::class,
        Persona::class,
        Item::class,
        Movimiento::class,
        Tag::class,
        ItemTag::class,
        ItemHistory::class,
        Logro::class,
        ItemDeseo::class      // ← añadido
    ],
    version = 7               // ← subir versión
)
@TypeConverters(DateConverter::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun coleccionDao(): ColeccionDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun personaDao(): PersonaDao
    abstract fun itemDao(): ItemDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun tagDao(): TagDao
    abstract fun itemTagDao(): ItemTagDao
    abstract fun itemHistoryDao(): ItemHistoryDao
    abstract fun logroDao(): LogroDao
    abstract fun itemDeseoDao(): ItemDeseoDao  // ← añadido
}