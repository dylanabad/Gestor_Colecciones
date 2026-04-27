package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase encargada de exportar la información de las colecciones a un documento PDF sencillo.
 *
 * A diferencia del catálogo visual, este exportador genera un listado textual estructurado
 * optimizado para la lectura rápida y la impresión, incluyendo metadatos detallados de cada ítem.
 *
 * @property context Contexto de la aplicación para acceder a directorios de almacenamiento.
 */
class PdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ── Paleta de colores ────────────────────────────────────────────────────
    private val colorAccent  = Color.rgb(30, 90, 160)
    private val colorHeading = Color.rgb(50, 50, 50)
    private val colorBody    = Color.rgb(90, 90, 90)
    private val colorMeta    = Color.rgb(130, 130, 130)
    private val colorLine    = Color.rgb(210, 210, 210)

    // ── Tipografía ───────────────────────────────────────────────────────────
    private val paintTitle = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f
        color    = colorAccent
        isAntiAlias = true
    }

    private val paintSection = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 13f
        color    = colorHeading
        isAntiAlias = true
    }

    private val paintBody = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = 10f
        color    = colorBody
        isAntiAlias = true
    }

    private val paintMeta = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 9f
        color    = colorMeta
        isAntiAlias = true
    }

    private val paintLine = Paint().apply {
        color       = colorLine
        strokeWidth = 0.8f
    }

    // ── Dimensiones de página (A4) ───────────────────────────────────────────
    private val pageWidth  = 595
    private val pageHeight = 842

    // ── Márgenes y espaciado ─────────────────────────────────────────────────
    private val marginH      = 48f
    private val marginV      = 36f
    private val lineHeight   = 16f
    private val sectionGap   = 10f
    private val itemIndent   = 14f
    private val metaIndent   = 22f

    /**
     * Genera un archivo PDF con la lista de colecciones e ítems proporcionada.
     *
     * @param data Datos de las colecciones a exportar.
     * @return Archivo PDF generado en el directorio de caché.
     */
    fun export(data: List<ColeccionExportData>): File {

        val document  = PdfDocument()
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page   = document.startPage(pageInfo)
        var canvas = page.canvas
        var y      = marginV + 20f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page   = document.startPage(pageInfo)
            canvas = page.canvas
            y      = marginV + 20f
        }

        fun checkSpace(needed: Float) {
            if (y + needed > pageHeight - marginV) newPage()
        }

        // ── Cabecera del documento ───────────────────────────────────────────
        canvas.drawText("Gestor de Colecciones — Exportación", marginH, y, paintTitle)
        y += 6f
        val accentLine = Paint().apply { color = colorAccent; strokeWidth = 2f }
        canvas.drawLine(marginH, y, pageWidth - marginH, y, accentLine)
        y += 24f

        // ── Colecciones ──────────────────────────────────────────────────────
        data.forEach { entry ->

            val c = entry.coleccion
            checkSpace(70f)

            y += sectionGap

            // Nombre de la colección
            canvas.drawText("■  ${c.nombre}", marginH, y, paintSection)
            y += lineHeight + 2f

            // Descripción (si existe)
            c.descripcion?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, marginH + itemIndent, y, paintBody)
                y += lineHeight
            }

            // Resumen de colección
            canvas.drawText(
                "Creada: ${dateFormat.format(c.fechaCreacion)}    " +
                        "Items: ${entry.items.size}    " +
                        "Valor total: ${"%.2f".format(entry.items.sumOf { it.valor })} €",
                marginH + itemIndent, y, paintMeta
            )
            y += lineHeight + 6f

            // ── Ítems ────────────────────────────────────────────────────────
            if (entry.items.isEmpty()) {
                canvas.drawText("(Sin items)", marginH + itemIndent, y, paintMeta)
                y += lineHeight
            } else {
                entry.items.forEach { item ->

                    checkSpace(lineHeight * 3 + 10f)

                    // Título del ítem
                    canvas.drawText(
                        "• ${item.titulo}",
                        marginH + itemIndent, y, paintBody
                    )
                    y += lineHeight

                    // Metadatos del ítem
                    canvas.drawText(
                        "Estado: ${item.estado}   " +
                                "Valor: ${"%.2f".format(item.valor)} €   " +
                                "★ ${"%.1f".format(item.calificacion)}   " +
                                "Adq: ${dateFormat.format(item.fechaAdquisicion)}",
                        marginH + metaIndent, y, paintMeta
                    )
                    y += lineHeight

                    // Descripción del ítem
                    item.descripcion?.takeIf { it.isNotBlank() }?.let {
                        canvas.drawText(it, marginH + metaIndent, y, paintBody)
                        y += lineHeight
                    }

                    y += 4f
                }
            }

            // Separador de sección
            y += 8f
            canvas.drawLine(marginH, y, pageWidth - marginH, y, paintLine)
            y += 12f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "colecciones_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }
}