package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import retrofit2.HttpException
import java.io.IOException

/**
 * Encapsula todas las operaciones de prestamos contra el backend.
 *
 * Ademas de consumir la API, mantiene sincronizado el flag local `prestado`
 * del item para que la UI refleje el estado real sin esperar a una sincronizacion completa.
 */
class PrestamoRepository(
    private val api: ApiService,
    private val itemDao: ItemDao
) {

    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }

    /** Recupera los posibles destinatarios de un prestamo. */
    suspend fun getUsuarios(): List<UsuarioDto> = try {
        api.getUsuarios()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            throw Exception("No autenticado. Por favor inicia sesion.")
        }
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener usuarios: ${e.message}")
    }

    /** Crea un nuevo prestamo y marca el item como prestado en Room. */
    suspend fun crearPrestamo(request: PrestamoRequest): PrestamoDto = try {
        val result = api.crearPrestamo(request)
        itemDao.updatePrestadoStatus(request.itemId.toInt(), true)
        result
    } catch (e: HttpException) {
        if (e.code() == 401) {
            throw Exception("No autenticado. Por favor inicia sesion.")
        }
        throw Exception("Error del servidor al crear el prestamo: ${e.message}")
    } catch (e: IOException) {
        throw Exception("Error de red al crear el prestamo: ${e.message}")
    }

    /** Marca un prestamo como devuelto y libera el item asociado en la cache local. */
    suspend fun devolverPrestamo(id: Long): PrestamoDto = try {
        val result = api.devolverPrestamo(id)
        itemDao.updatePrestadoStatus(result.itemId.toInt(), false)
        result
    } catch (e: HttpException) {
        if (e.code() == 401) {
            throw Exception("No autenticado. Por favor inicia sesion.")
        }
        throw Exception("Error del servidor al devolver el prestamo: ${e.message}")
    } catch (e: IOException) {
        throw Exception("Error de red al devolver el prestamo: ${e.message}")
    }

    /** Elimina definitivamente un prestamo del sistema. */
    suspend fun deletePrestamo(id: Long) {
        try {
            api.deletePrestamoHard(id)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw Exception("No autenticado. Por favor inicia sesion.")
            }
            if (e.code() == 403) {
                throw Exception("No tienes permisos para eliminar este prestamo")
            }
            throw Exception(extractError(e))
        } catch (e: IOException) {
            throw Exception("Error de red al eliminar el prestamo: ${e.message}")
        }
    }

    /** Recupera los prestamos enviados por el usuario autenticado. */
    suspend fun getPrestados(): List<PrestamoDto> = try {
        api.getPrestados()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            throw Exception("No autenticado. Por favor inicia sesion.")
        }
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener prestamos: ${e.message}")
    }

    /** Recupera los prestamos recibidos por el usuario autenticado. */
    suspend fun getPrestamosRecibidos(): List<PrestamoDto> = try {
        api.getPrestamosRecibidos()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            throw Exception("No autenticado. Por favor inicia sesion.")
        }
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener prestamos recibidos: ${e.message}")
    }
}
