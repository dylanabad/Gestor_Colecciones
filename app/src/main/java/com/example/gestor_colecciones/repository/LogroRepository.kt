package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.LogroDao
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.DateMapper
import com.example.gestor_colecciones.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class LogroRepository(
    private val logroDao: LogroDao,
    private val api: ApiService
) {

    val allLogros: Flow<List<Logro>> = logroDao.getAllLogros()

    // Inicializa todos los logros en BD si no existen aun
    suspend fun initLogros() {
        LogroDefinicion.TODOS.forEach { info ->
            logroDao.insert(Logro(key = info.key, desbloqueado = false))
        }
    }

    suspend fun syncFromBackend() {
        runCatching {
            val remotos = api.getLogros()
            if (remotos.isEmpty()) return
            remotos.forEach { dto ->
                val existing = logroDao.getByKey(dto.key)
                if (existing == null) {
                    logroDao.insert(dto.toEntity())
                } else if (dto.desbloqueado && !existing.desbloqueado) {
                    logroDao.update(
                        existing.copy(
                            desbloqueado = true,
                            fechaDesbloqueo = DateMapper.parse(dto.fechaDesbloqueo) ?: existing.fechaDesbloqueo
                        )
                    )
                }
            }
        }
    }

    suspend fun desbloquear(key: String): Boolean {
        val existing = logroDao.getByKey(key)
        if (existing == null || existing.desbloqueado) return false
        val now = Date()
        val remoto = runCatching { api.unlockLogro(key) }.getOrNull()
        val fecha = remoto?.fechaDesbloqueo?.let { DateMapper.parse(it) } ?: now
        logroDao.update(existing.copy(desbloqueado = true, fechaDesbloqueo = fecha))
        return true  // true = recien desbloqueado
    }

    suspend fun estaDesbloqueado(key: String): Boolean =
        logroDao.getByKey(key)?.desbloqueado == true

    suspend fun countDesbloqueados(): Int = logroDao.countDesbloqueados()
}
