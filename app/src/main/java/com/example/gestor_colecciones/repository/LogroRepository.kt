package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.LogroDao
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.DateMapper
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repositorio encargado de gestionar los logros (achievements)
 * sincronizando estado local con el backend
 */
class LogroRepository(
    private val logroDao: LogroDao, // Acceso a BD local de logros
    private val api: ApiService     // Acceso a API remota
) {

    /**
     * Flujo con todos los logros almacenados localmente
     */
    val allLogros: Flow<List<Logro>> = logroDao.getAllLogros()

    /**
     * Inicializa los logros definidos en la app si aún no existen en la BD
     */
    suspend fun initLogros() {

        // Recorre la definición estática de logros
        LogroDefinicion.TODOS.forEach { info ->

            // Inserta cada logro con estado inicial desbloqueado = false
            logroDao.insert(
                Logro(
                    key = info.key,
                    desbloqueado = false
                )
            )
        }
    }

    /**
     * Sincroniza logros desde el backend hacia la base de datos local
     */
    suspend fun syncFromBackend() {

        runCatching {

            /**
             * Obtiene lista de logros remotos
             */
            val remotos = api.getLogros()

            // Si no hay datos remotos, termina la sincronización
            if (remotos.isEmpty()) return

            // Recorre cada logro recibido del servidor
            remotos.forEach { dto ->

                /**
                 * Busca si ya existe localmente
                 */
                val existing = logroDao.getByKey(dto.key)

                if (existing == null) {

                    // Si no existe, lo inserta directamente
                    logroDao.insert(dto.toEntity())

                } else if (dto.desbloqueado && !existing.desbloqueado) {

                    // Si está desbloqueado en remoto pero no en local, actualiza estado
                    logroDao.update(
                        existing.copy(
                            desbloqueado = true,

                            // Convierte fecha del backend a Date local
                            fechaDesbloqueo =
                                DateMapper.parse(dto.fechaDesbloqueo)
                                    ?: existing.fechaDesbloqueo
                        )
                    )
                }
            }
        }
    }

    /**
     * Desbloquea un logro específico
     */
    suspend fun desbloquear(key: String): Boolean {

        /**
         * Obtiene el logro local
         */
        val existing = logroDao.getByKey(key)

        // Si no existe o ya está desbloqueado, no hace nada
        if (existing == null || existing.desbloqueado) return false

        /**
         * Fecha actual como fallback
         */
        val now = Date()

        /**
         * Intenta notificar al backend el desbloqueo
         */
        val remoto = runCatching { api.unlockLogro(key) }.getOrNull()

        /**
         * Usa la fecha del backend si existe, si no la local
         */
        val fecha =
            remoto?.fechaDesbloqueo?.let { DateMapper.parse(it) } ?: now

        // Actualiza el logro en base de datos local
        logroDao.update(
            existing.copy(
                desbloqueado = true,
                fechaDesbloqueo = fecha
            )
        )

        // Indica que se desbloqueó en esta operación
        return true
    }

    /**
     * Comprueba si un logro está desbloqueado
     */
    suspend fun estaDesbloqueado(key: String): Boolean =
        logroDao.getByKey(key)?.desbloqueado == true

    /**
     * Cuenta cuántos logros están desbloqueados
     */
    suspend fun countDesbloqueados(): Int =
        logroDao.countDesbloqueados()
}