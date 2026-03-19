package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val paintTitle = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = Color.BLACK
    }
    private val paintSection = Paint().apply {
        textSize = 14f
        isFakeBoldText = true
        color = Color.DKGRAY
    }
    private val paintBody = Paint().apply {
        textSize = 11f
        color = Color.DKGRAY
    }
    private val paintLine = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    private val pageWidth = 595   // A4 puntos
    private val pageHeight = 842
    private val margin = 40f
    private val lineHeight = 18f

    fun export(data: List<ColeccionExportData>): File {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 20f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 20f
        }

        fun checkSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        // Título principal
        canvas.drawText("Gestor de Colecciones — Exportación", margin, y, paintTitle)
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, paintLine)
        y += 20f

        data.forEach { entry ->
            val c = entry.coleccion

            checkSpace(60f)

            // Nombre colección
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
                margin, y, paintBody
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
                        margin, y, paintBody
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

        document.finishPage(page)

        val file = File(context.cacheDir, "colecciones_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}