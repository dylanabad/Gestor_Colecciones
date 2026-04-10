package com.example.gestor_colecciones.network.dto

import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.DateMapper
import java.util.Date

// DTO que representa un item en la comunicación con la API
data class ItemDto(
    val id: Long? = null,                      // Identificador opcional del item
    val titulo: String,                        // Título del item
    val valor: Double? = 0.0,                  // Valor económico del item
    val fechaAdquisicion: String? = null,      // Fecha de adquisición en formato String (API)
    val imagenPath: String? = null,            // Ruta de la imagen asociada
    val estado: String? = null,                // Estado del item (nuevo, usado, etc.)
    val descripcion: String? = null,           // Descripción opcional
    val calificacion: Float? = 0f,            // Valoración del item
    val eliminado: Boolean? = false,           // Flag de eliminación lógica
    val fechaEliminacion: String? = null,      // Fecha de eliminación en formato String
    val prestado: Boolean? = false,            // Indica si el item está prestado
    val favorito: Boolean? = false,            // Indica si está marcado como favorito
    val categoria: CategoriaDto? = null        // Categoría asociada al item
)

// Convierte DTO recibido desde la API a entidad local
fun ItemDto.toEntity(collectionId: Int): Item {
    return Item(
        id = id?.toInt() ?: 0, // Si no hay id, se asigna 0
        titulo = titulo,
        categoriaId = categoria?.id?.toInt() ?: 0,
        collectionId = collectionId, // Se pasa explícitamente la colección
        fechaAdquisicion = DateMapper.parse(fechaAdquisicion) ?: Date(),
        valor = valor ?: 0.0,
        imagenPath = imagenPath,
        estado = estado.orEmpty(),
        descripcion = descripcion,
        calificacion = calificacion ?: 0f,
        eliminado = eliminado ?: false,
        fechaEliminacion = DateMapper.parse(fechaEliminacion),
        prestado = prestado ?: false,
        favorito = favorito ?: false
    )
}

// Convierte entidad local a DTO para enviar a la API
fun Item.toDto(): ItemDto {
    return ItemDto(
        id = id.takeIf { it > 0 }?.toLong(), // Solo envía id si es válido
        titulo = titulo,
        valor = valor,
        fechaAdquisicion = DateMapper.format(fechaAdquisicion),
        imagenPath = imagenPath,
        estado = estado,
        descripcion = descripcion,
        calificacion = calificacion,
        eliminado = eliminado,
        fechaEliminacion = DateMapper.format(fechaEliminacion),
        prestado = prestado,
        favorito = favorito
    )
}