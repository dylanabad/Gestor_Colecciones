package com.example.gestor_colecciones.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gestor_colecciones.dao.ColeccionDao
import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.dao.CategoriaDao

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

    fun getDatabase(context: Context): AppDataBase {
        return instance ?: synchronized(this) {

            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                "gestor_colecciones_db"
            )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration() // recrea la BD si cambia el esquema (si falta migraciÃ³n)
                .build()

            instance = db
            db
        }
    }

    // Acceso rápido a DAOs
    fun getColeccionDao(context: Context): ColeccionDao =
        getDatabase(context).coleccionDao()

    fun getItemDao(context: Context): ItemDao =
        getDatabase(context).itemDao()

    fun getCategoriaDao(context: Context): CategoriaDao =
        getDatabase(context).categoriaDao()
}
