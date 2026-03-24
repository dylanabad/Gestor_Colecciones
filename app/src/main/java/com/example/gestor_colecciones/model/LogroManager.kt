package com.example.gestor_colecciones.model

import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.LogroRepository

class LogroManager(
    private val logroRepository: LogroRepository,
    private val coleccionRepository: ColeccionRepository,
    private val itemRepository: ItemRepository
) {
    // Devuelve lista de keys recién desbloqueados para mostrar notificación
    suspend fun checkAll(): List<String> {
        logroRepository.initLogros()
        logroRepository.syncFromBackend()
        val recienDesbloqueados = mutableListOf<String>()

        val colecciones = coleccionRepository.getAllOnce()
        val totalItems = itemRepository.getTotalItems()
        val totalValor = itemRepository.getTotalValor()

        // ── Colecciones ───────────────────────────────────────────────
        if (colecciones.size >= 1)
            if (logroRepository.desbloquear("PRIMERA_COLECCION"))
                recienDesbloqueados.add("PRIMERA_COLECCION")

        if (colecciones.size >= 5)
            if (logroRepository.desbloquear("COLECCIONES_5"))
                recienDesbloqueados.add("COLECCIONES_5")

        if (colecciones.size >= 10)
            if (logroRepository.desbloquear("COLECCIONES_10"))
                recienDesbloqueados.add("COLECCIONES_10")

        // ── Items ─────────────────────────────────────────────────────
        if (totalItems >= 1)
            if (logroRepository.desbloquear("PRIMER_ITEM"))
                recienDesbloqueados.add("PRIMER_ITEM")

        if (totalItems >= 10)
            if (logroRepository.desbloquear("ITEMS_10"))
                recienDesbloqueados.add("ITEMS_10")

        if (totalItems >= 50)
            if (logroRepository.desbloquear("ITEMS_50"))
                recienDesbloqueados.add("ITEMS_50")

        if (totalItems >= 100)
            if (logroRepository.desbloquear("ITEMS_100"))
                recienDesbloqueados.add("ITEMS_100")

        // ── Valor ─────────────────────────────────────────────────────
        if (totalValor >= 1000.0)
            if (logroRepository.desbloquear("VALOR_1000"))
                recienDesbloqueados.add("VALOR_1000")

        if (totalValor >= 10000.0)
            if (logroRepository.desbloquear("VALOR_10000"))
                recienDesbloqueados.add("VALOR_10000")

        // ── Imagen ────────────────────────────────────────────────────
        val tieneImagen = colecciones.any { it.imagenPath != null }
        if (tieneImagen)
            if (logroRepository.desbloquear("PRIMERA_IMAGEN"))
                recienDesbloqueados.add("PRIMERA_IMAGEN")

        // ── Colección con 20+ items ───────────────────────────────────
        val tieneColeccionGrande = colecciones.any { coleccion ->
            itemRepository.getItemsByCollectionOnce(coleccion.id).size >= 20
        }
        if (tieneColeccionGrande)
            if (logroRepository.desbloquear("COLECCION_20_ITEMS"))
                recienDesbloqueados.add("COLECCION_20_ITEMS")

        // ── Item con calificación 5 ───────────────────────────────────
        val items = colecciones.flatMap { itemRepository.getItemsByCollectionOnce(it.id) }
        if (items.any { it.calificacion >= 5f })
            if (logroRepository.desbloquear("CALIFICACION_5"))
                recienDesbloqueados.add("CALIFICACION_5")

        return recienDesbloqueados
    }
}
