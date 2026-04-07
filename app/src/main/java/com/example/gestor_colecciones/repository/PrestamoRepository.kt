package com.example.gestor_colecciones.repository

import com.example.gestor_colecciones.network.ApiService
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import retrofit2.HttpException
import java.io.IOException

class PrestamoRepository(private val api: ApiService) {

    suspend fun getUsuarios(): List<UsuarioDto> = try {
        api.getUsuarios()
    } catch (e: HttpException) {
        if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
        // En otros errores HTTP devolvemos lista vacía para evitar romper la UI
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener usuarios: ${e.message}")
    }

    suspend fun crearPrestamo(request: PrestamoRequest): PrestamoDto = try {
        api.crearPrestamo(request)
    } catch (e: HttpException) {
        if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
        throw Exception("Error del servidor al crear el préstamo: ${e.message}")
    } catch (e: IOException) {
        throw Exception("Error de red al crear el préstamo: ${e.message}")
    }

    suspend fun devolverPrestamo(id: Long): PrestamoDto = try {
        api.devolverPrestamo(id)
    } catch (e: HttpException) {
        if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
        throw Exception("Error del servidor al devolver el préstamo: ${e.message}")
    } catch (e: IOException) {
        throw Exception("Error de red al devolver el préstamo: ${e.message}")
    }

    suspend fun deletePrestamo(id: Long) {
        try {
            api.deletePrestamo(id)
        } catch (e: HttpException) {
            if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
            if (e.code() == 403) throw Exception("No tienes permisos para eliminar este préstamo")
            throw Exception("Error del servidor al eliminar el préstamo: ${e.message}")
        } catch (e: IOException) {
            throw Exception("Error de red al eliminar el préstamo: ${e.message}")
        }
    }

    suspend fun getPrestados(): List<PrestamoDto> = try {
        api.getPrestados()
    } catch (e: HttpException) {
        if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
        // Si el servidor devuelve error (p. ej. NPE interno) consideramos que no hay préstamos
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener préstamos: ${e.message}")
    }

    suspend fun getPrestamosRecibidos(): List<PrestamoDto> = try {
        api.getPrestamosRecibidos()
    } catch (e: HttpException) {
        if (e.code() == 401) throw Exception("No autenticado. Por favor inicia sesión.")
        emptyList()
    } catch (e: IOException) {
        throw Exception("Error de red al obtener préstamos recibidos: ${e.message}")
    }
}