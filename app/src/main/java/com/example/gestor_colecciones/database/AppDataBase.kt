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
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.converter.DateConverter
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.dao.PersonaDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.dao.MovimientoDao
import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.dao.ItemHistoryDao
import com.example.gestor_colecciones.dao.LogroDao
import com.example.gestor_colecciones.dao.ItemDeseoDao

// Base de datos principal de la aplicación usando Room
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
        ItemDeseo::class
    ],
    version = 11
)
@TypeConverters(DateConverter::class)
abstract class AppDataBase : RoomDatabase() {

    // DAO de colecciones
    abstract fun coleccionDao(): ColeccionDao

    // DAO de categorías
    abstract fun categoriaDao(): CategoriaDao

    // DAO de personas
    abstract fun personaDao(): PersonaDao

    // DAO de items
    abstract fun itemDao(): ItemDao

    // DAO de movimientos (préstamos, etc.)
    abstract fun movimientoDao(): MovimientoDao

    // DAO de etiquetas
    abstract fun tagDao(): TagDao

    // DAO de relación item-tag
    abstract fun itemTagDao(): ItemTagDao

    // DAO de historial de items
    abstract fun itemHistoryDao(): ItemHistoryDao

    // DAO de logros
    abstract fun logroDao(): LogroDao

    // DAO de lista de deseos
    abstract fun itemDeseoDao(): ItemDeseoDao
}
