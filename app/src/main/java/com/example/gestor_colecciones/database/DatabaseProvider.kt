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

object DatabaseProvider {

    private var instance: AppDataBase? = null

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

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
        }
    }

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

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `eliminado` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `Coleccion` ADD COLUMN `fechaEliminacion` INTEGER")
            db.execSQL("ALTER TABLE `Item` ADD COLUMN `eliminado` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `Item` ADD COLUMN `fechaEliminacion` INTEGER")
        }
    }

    fun getDatabase(context: Context): AppDataBase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "gestor_colecciones_db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()

            instance = db
            db
        }
    }

    fun getColeccionDao(context: Context): ColeccionDao = getDatabase(context).coleccionDao()
    fun getItemDao(context: Context): ItemDao = getDatabase(context).itemDao()
    fun getCategoriaDao(context: Context): CategoriaDao = getDatabase(context).categoriaDao()
    fun getTagDao(context: Context): TagDao = getDatabase(context).tagDao()
    fun getItemTagDao(context: Context): ItemTagDao = getDatabase(context).itemTagDao()
}
