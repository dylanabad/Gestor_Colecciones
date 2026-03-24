package com.example.gestor_colecciones.repository

import android.content.Context
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.network.ApiProvider

object RepositoryProvider {
    fun coleccionRepository(context: Context): ColeccionRepository {
        return ColeccionRepository(
            DatabaseProvider.getColeccionDao(context),
            ApiProvider.getApi(context)
        )
    }

    fun itemRepository(context: Context): ItemRepository {
        return ItemRepository(
            DatabaseProvider.getItemDao(context),
            ApiProvider.getApi(context)
        )
    }

    fun categoriaRepository(context: Context): CategoriaRepository {
        return CategoriaRepository(
            DatabaseProvider.getCategoriaDao(context),
            ApiProvider.getApi(context)
        )
    }

    fun papeleraRepository(context: Context): PapeleraRepository {
        val db = DatabaseProvider.getDatabase(context)
        return PapeleraRepository(
            db.coleccionDao(),
            db.itemDao(),
            ApiProvider.getApi(context)
        )
    }

    fun syncRepository(context: Context): SyncRepository {
        return SyncRepository(
            ApiProvider.getApi(context),
            DatabaseProvider.getDatabase(context)
        )
    }

    fun tagRepository(context: Context): TagRepository {
        return TagRepository(
            DatabaseProvider.getTagDao(context),
            ApiProvider.getApi(context)
        )
    }

    fun itemTagRepository(context: Context): ItemTagRepository {
        val db = DatabaseProvider.getDatabase(context)
        return ItemTagRepository(
            db.itemTagDao(),
            db.tagDao(),
            ApiProvider.getApi(context)
        )
    }

    fun itemDeseoRepository(context: Context): ItemDeseoRepository {
        return ItemDeseoRepository(
            DatabaseProvider.getDatabase(context).itemDeseoDao(),
            ApiProvider.getApi(context)
        )
    }
}
