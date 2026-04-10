package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Estilo del título principal del PDF
    private val paintTitle = Paint().apply {
        textSize = 18f; isFakeBoldText = true; color = Color.BLACK
    }

    // Estilo para títulos de sección (colecciones)
    private val paintSection = Paint().apply {
        textSize = 14f; isFakeBoldText = true; color = Color.DKGRAY
    }

    // Estilo para texto general del cuerpo
    private val paintBody = Paint().apply {
        textSize = 11f; color = Color.DKGRAY
    }

    // Línea separadora visual
    private val paintLine = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 1f
    }

    // Dimensiones de página tipo A4
    private val pageWidth = 595
    private val pageHeight = 842

    // Márgenes y espaciado base
    private val margin = 40f
    private val lineHeight = 18f

    // Exporta la información a un PDF
    fun export(data: List<ColeccionExportData>): File {

        val document = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 20f

        // Crea una nueva página cerrando la anterior
        fun newPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 20f
        }

        // Comprueba si hay espacio suficiente en la página actual
        fun checkSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        // Título principal del documento
        canvas.drawText("Gestor de Colecciones — Exportación", margin, y, paintTitle)
        y += 10f

        canvas.drawLine(margin, y, pageWidth - margin, y, paintLine)
        y += 20f

        // Itera colecciones
        data.forEach { entry ->

            val c = entry.coleccion
            checkSpace(60f)

            canvas.drawText("■  ${c.nombre}", margin, y, paintSection)
            y += lineHeight

            c.descripcion?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText("   Descripción: $it", margin, y, paintBody)
                y += lineHeight
            }

            canvas.drawText(
                "   Creada: ${dateFormat.format(c.fechaCreacion)}  |  " +
                        "Items: ${entry.items.size}  |  " +
                        "Valor total: ${"%.2f".format(entry.items.sumOf { it.valor })} €",
                margin,
                y,
                paintBody
            )

            y += lineHeight + 4f

            if (entry.items.isEmpty()) {

                canvas.drawText("   (Sin items)", margin, y, paintBody)
                y += lineHeight

            } else {

                entry.items.forEach { item ->

                    checkSpace(lineHeight * 2 + 8f)

                    canvas.drawText("   • ${item.titulo}", margin, y, paintBody)
                    y += lineHeight

                    canvas.drawText(
                        "     Estado: ${item.estado}  |  " +
                                "Valor: ${"%.2f".format(item.valor)} €  |  " +
                                "★ ${"%.1f".format(item.calificacion)}  |  " +
                                "Adq: ${dateFormat.format(item.fechaAdquisicion)}",
                        margin,
                        y,
                        paintBody
                    )

                    y += lineHeight

                    item.descripcion?.takeIf { it.isNotBlank() }?.let {
                        canvas.drawText("     $it", margin, y, paintBody)
                        y += lineHeight
                    }

                    y += 4f
                }
            }

            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, paintLine)
            y += 14f
        }

        // Cierra la última página y guarda el PDF
        document.finishPage(page)

        val file = File(context.cacheDir, "colecciones_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }
}