package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.dao.ItemDao
import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import retrofit2.HttpException
import java.io.IOException

// Repositorio encargado de gestionar operaciones relacionadas con préstamos
// usando exclusivamente la API remota
class PrestamoRepository(
    private val api: ApiService,
    private val itemDao: ItemDao
) {

    private fun extractError(e: HttpException): String {
        val body = e.response()?.errorBody()?.string()
        return if (body.isNullOrBlank()) "HTTP ${e.code()}" else body
    }

    // Obtiene la lista de usuarios desde el servidor
    suspend fun getUsuarios(): List<UsuarioDto> = try {

        api.getUsuarios()

    } catch (e: HttpException) {

        // Si no está autenticado, lanza un error específico
        if (e.code() == 401)
            throw Exception("No autenticado. Por favor inicia sesión.")

        // En otros errores HTTP devuelve lista vacía para no romper la UI
        emptyList()

    } catch (e: IOException) {

        // Error de red (sin conexión, timeout, etc.)
        throw Exception("Error de red al obtener usuarios: ${e.message}")
    }

    // Crea un nuevo préstamo en el servidor
    suspend fun crearPrestamo(request: PrestamoRequest): PrestamoDto = try {

        val result = api.crearPrestamo(request)
        // Marcamos como prestado localmente
        itemDao.updatePrestadoStatus(request.itemId.toInt(), true)
        result

    } catch (e: HttpException) {

        if (e.code() == 401)
            throw Exception("No autenticado. Por favor inicia sesión.")

        throw Exception("Error del servidor al crear el préstamo: ${e.message}")

    } catch (e: IOException) {

        throw Exception("Error de red al crear el préstamo: ${e.message}")
    }

    // Marca un préstamo como devuelto
    suspend fun devolverPrestamo(id: Long): PrestamoDto = try {

        val result = api.devolverPrestamo(id)
        // Actualizamos localmente el estado del item si tenemos el ID
        itemDao.updatePrestadoStatus(result.itemId.toInt(), false)
        result

    } catch (e: HttpException) {

        if (e.code() == 401)
            throw Exception("No autenticado. Por favor inicia sesión.")

        throw Exception("Error del servidor al devolver el préstamo: ${e.message}")

    } catch (e: IOException) {

        throw Exception("Error de red al devolver el préstamo: ${e.message}")
    }

    // Elimina un préstamo del sistema
    suspend fun deletePrestamo(id: Long) {
        try {

            // Eliminación definitiva para que desaparezca realmente de la base de datos
            api.deletePrestamoHard(id)

        } catch (e: HttpException) {

            if (e.code() == 401)
                throw Exception("No autenticado. Por favor inicia sesión.")

            if (e.code() == 403)
                throw Exception("No tienes permisos para eliminar este préstamo")

            throw Exception(extractError(e))

        } catch (e: IOException) {

            throw Exception("Error de red al eliminar el préstamo: ${e.message}")
        }
    }

    // Obtiene los préstamos enviados por el usuario
    suspend fun getPrestados(): List<PrestamoDto> = try {

        api.getPrestados()

    } catch (e: HttpException) {

        if (e.code() == 401)
            throw Exception("No autenticado. Por favor inicia sesión.")

        // En caso de error interno, se devuelve lista vacía
        emptyList()

    } catch (e: IOException) {

        throw Exception("Error de red al obtener préstamos: ${e.message}")
    }

    // Obtiene los préstamos recibidos por el usuario
    suspend fun getPrestamosRecibidos(): List<PrestamoDto> = try {

        api.getPrestamosRecibidos()

    } catch (e: HttpException) {

        if (e.code() == 401)
            throw Exception("No autenticado. Por favor inicia sesión.")

        emptyList()

    } catch (e: IOException) {

        throw Exception("Error de red al obtener préstamos recibidos: ${e.message}")
    }
}
