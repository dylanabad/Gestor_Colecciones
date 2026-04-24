package com.example.gestor_colecciones.repository

import android.content.Context
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.network.ApiProvider

/**
 * Punto unico de creacion de repositorios.
 *
 * Mantiene la construccion de dependencias fuera de fragments y activities para
 * reducir duplicacion y asegurar que cada repositorio recibe el DAO y la API correctos.
 */
object RepositoryProvider {

    /** Construye el repositorio de colecciones usando el contexto de aplicacion. */
    fun coleccionRepository(context: Context): ColeccionRepository {
        return ColeccionRepository(
            context.applicationContext,
            DatabaseProvider.getColeccionDao(context),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio de items. */
    fun itemRepository(context: Context): ItemRepository {
        return ItemRepository(
            context.applicationContext,
            DatabaseProvider.getItemDao(context),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio de categorias. */
    fun categoriaRepository(context: Context): CategoriaRepository {
        return CategoriaRepository(
            DatabaseProvider.getCategoriaDao(context),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio de papelera con acceso a colecciones, items y deseos. */
    fun papeleraRepository(context: Context): PapeleraRepository {
        val db = DatabaseProvider.getDatabase(context)

        return PapeleraRepository(
            context.applicationContext,
            db.coleccionDao(),
            db.itemDao(),
            db.itemDeseoDao(),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio responsable de sincronizar la cache local. */
    fun syncRepository(context: Context): SyncRepository {
        return SyncRepository(
            ApiProvider.getApi(context),
            DatabaseProvider.getDatabase(context)
        )
    }

    /** Construye el repositorio de etiquetas. */
    fun tagRepository(context: Context): TagRepository {
        return TagRepository(
            DatabaseProvider.getTagDao(context),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio que gestiona la relacion entre items y etiquetas. */
    fun itemTagRepository(context: Context): ItemTagRepository {
        val db = DatabaseProvider.getDatabase(context)

        return ItemTagRepository(
            db.itemTagDao(),
            db.tagDao(),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio de lista de deseos. */
    fun itemDeseoRepository(context: Context): ItemDeseoRepository {
        return ItemDeseoRepository(
            DatabaseProvider.getDatabase(context).itemDeseoDao(),
            ApiProvider.getApi(context)
        )
    }

    /** Construye el repositorio de prestamos. */
    fun prestamoRepository(context: Context): PrestamoRepository {
        return PrestamoRepository(
            ApiProvider.getApi(context),
            DatabaseProvider.getItemDao(context)
        )
    }
}
