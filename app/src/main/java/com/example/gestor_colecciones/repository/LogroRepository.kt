package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.LogroDao
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import kotlinx.coroutines.flow.Flow
import java.util.Date

class LogroRepository(private val logroDao: LogroDao) {

    val allLogros: Flow<List<Logro>> = logroDao.getAllLogros()

    // Inicializa todos los logros en BD si no existen aún
    suspend fun initLogros() {
        LogroDefinicion.TODOS.forEach { info ->
            logroDao.insert(Logro(key = info.key, desbloqueado = false))
        }
    }

    suspend fun desbloquear(key: String): Boolean {
        val existing = logroDao.getByKey(key)
        if (existing == null || existing.desbloqueado) return false
        logroDao.update(existing.copy(desbloqueado = true, fechaDesbloqueo = Date()))
        return true  // true = recién desbloqueado
    }

    suspend fun estaDesbloqueado(key: String): Boolean =
        logroDao.getByKey(key)?.desbloqueado == true

    suspend fun countDesbloqueados(): Int = logroDao.countDesbloqueados()
}