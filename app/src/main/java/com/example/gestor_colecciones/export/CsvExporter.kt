package com.example.gestor_colecciones.export

import android.content.Context
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun export(data: List<ColeccionExportData>): File {
        val sb = StringBuilder()

        // Cabecera colecciones
        sb.appendLine("=== COLECCIONES ===")
        sb.appendLine("ID,Nombre,Descripcion,Fecha Creacion,Total Items,Valor Total")

        data.forEach { entry ->
            val c = entry.coleccion
            sb.appendLine(
                "${c.id}," +
                        "\"${c.nombre}\"," +
                        "\"${c.descripcion ?: ""}\"," +
                        "${dateFormat.format(c.fechaCreacion)}," +
                        "${entry.items.size}," +
                        "${entry.items.sumOf { it.valor }}"
            )
        }

        sb.appendLine()

        // Cabecera items
        sb.appendLine("=== ITEMS ===")
        sb.appendLine("ID,Coleccion,Titulo,Estado,Valor,Calificacion,Fecha Adquisicion,Descripcion")

        data.forEach { entry ->
            entry.items.forEach { item ->
                sb.appendLine(
                    "${item.id}," +
                            "\"${entry.coleccion.nombre}\"," +
                            "\"${item.titulo}\"," +
                            "${item.estado}," +
                            "${item.valor}," +
                            "${item.calificacion}," +
                            "${dateFormat.format(item.fechaAdquisicion)}," +
                            "\"${item.descripcion ?: ""}\""
                )
            }
        }

        val file = File(context.cacheDir, "colecciones_export.csv")
        file.writeText(sb.toString())
        return file
    }
}