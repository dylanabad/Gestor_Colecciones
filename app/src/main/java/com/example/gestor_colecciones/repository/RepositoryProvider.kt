package com.example.gestor_colecciones.repository

import android.content.Context
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.network.ApiProvider

// Proveedor central de instancias de repositorios
// Se encarga de crear e inyectar dependencias (DAO + API) en cada repositorio
object RepositoryProvider {

    // Crea instancia del repositorio de colecciones
    fun coleccionRepository(context: Context): ColeccionRepository {
        return ColeccionRepository(
            DatabaseProvider.getColeccionDao(context),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de items
    fun itemRepository(context: Context): ItemRepository {
        return ItemRepository(
            DatabaseProvider.getItemDao(context),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de categorías
    fun categoriaRepository(context: Context): CategoriaRepository {
        return CategoriaRepository(
            DatabaseProvider.getCategoriaDao(context),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de papelera (soft delete y restauración)
    fun papeleraRepository(context: Context): PapeleraRepository {

        val db = DatabaseProvider.getDatabase(context)

        return PapeleraRepository(
            db.coleccionDao(),
            db.itemDao(),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de sincronización
    fun syncRepository(context: Context): SyncRepository {
        return SyncRepository(
            ApiProvider.getApi(context),
            DatabaseProvider.getDatabase(context)
        )
    }

    // Crea instancia del repositorio de tags
    fun tagRepository(context: Context): TagRepository {
        return TagRepository(
            DatabaseProvider.getTagDao(context),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de relación item-tag
    fun itemTagRepository(context: Context): ItemTagRepository {

        val db = DatabaseProvider.getDatabase(context)

        return ItemTagRepository(
            db.itemTagDao(),
            db.tagDao(),
            ApiProvider.getApi(context)
        )
    }

    // Crea instancia del repositorio de lista de deseos
    fun itemDeseoRepository(context: Context): ItemDeseoRepository {
        return ItemDeseoRepository(
            DatabaseProvider.getDatabase(context).itemDeseoDao(),
            ApiProvider.getApi(context)
        )
    }
}