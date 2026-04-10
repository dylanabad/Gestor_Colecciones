package com.example.gestor_colecciones.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.dao.CategoriaDao
import com.example.gestor_colecciones.dao.TagDao
import com.example.gestor_colecciones.dao.ItemTagDao
import com.example.gestor_colecciones.dao.ItemDeseoDao

// Proveedor único de la base de datos (singleton)
object DatabaseProvider {

    // Instancia única de la base de datos
    private var instance: AppDataBase? = null

    // Migración de la versión 3 a 4: creación de tabla de historial de items
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `item_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `itemId` INTEGER NOT NULL,
                    `tipo` TEXT NOT NULL,
                    `fecha` INTEGER NOT NULL,
                    `descripcion` TEXT,
                    FOREIGN KEY(`itemId`) REFERENCES `Item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_history_itemId` ON `item_history` (`itemId`)")
        }
    }

    // Migración 4 → 5: añadir color a colecciones
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
        }
    }

    // Migración 5 → 6: creación de tabla Logro
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `Logro` (
                    `key` TEXT NOT NULL PRIMARY KEY,
                    `desbloqueado` INTEGER NOT NULL DEFAULT 0,
                    `fechaDesbloqueo` INTEGER
                )
                """.trimIndent()
            )
        }
    }

    // Migración 6 → 7: creación de tabla de deseos
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ItemDeseo` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `titulo` TEXT NOT NULL,
                    `descripcion` TEXT,
                    `precioObjetivo` REAL NOT NULL DEFAULT 0.0,
                    `prioridad` INTEGER NOT NULL DEFAULT 2,
                    `enlace` TEXT,
                    `conseguido` INTEGER NOT NULL DEFAULT 0,
                    `fechaCreacion` INTEGER NOT NULL,
                    `fechaConseguido` INTEGER
                )
                """.trimIndent()
            )
        }
    }

    // Migración 7 → 8: campos de eliminación lógica
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `eliminado` INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `fechaEliminacion` INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Item` ADD COLUMN `eliminado` INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Item` ADD COLUMN `fechaEliminacion` INTEGER")
            } catch (_: Exception) {}
        }
    }

    // Migración 8 → 9: mejoras en préstamos y personas
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE `Item` ADD COLUMN `prestado` INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Persona` ADD COLUMN `usuarioId` INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Persona` ADD COLUMN `usuarioRefId` INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Movimiento` ADD COLUMN `estado` TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Movimiento` ADD COLUMN `fechaDevolucionPrevista` INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Movimiento` ADD COLUMN `fechaDevolucionReal` INTEGER")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE `Movimiento` ADD COLUMN `notas` TEXT")
            } catch (_: Exception) {}
        }
    }

    // Migración 9 → 10: favorito en items
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE `Item` ADD COLUMN `favorito` INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
        }
    }

    // Obtiene la instancia de la base de datos (singleton thread-safe)
    fun getDatabase(context: Context): AppDataBase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "gestor_colecciones_db"
            )
                .addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                .fallbackToDestructiveMigration()
                .build()

            instance = db
            db
        }
    }

    // Helpers para acceso directo a DAOs desde el proveedor
    fun getColeccionDao(context: Context): ColeccionDao = getDatabase(context).coleccionDao()
    fun getItemDao(context: Context): ItemDao = getDatabase(context).itemDao()
    fun getCategoriaDao(context: Context): CategoriaDao = getDatabase(context).categoriaDao()
    fun getTagDao(context: Context): TagDao = getDatabase(context).tagDao()
    fun getItemTagDao(context: Context): ItemTagDao = getDatabase(context).itemTagDao()
    fun getItemDeseoDao(context: Context): ItemDeseoDao = getDatabase(context).itemDeseoDao()
}