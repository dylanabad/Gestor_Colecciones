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

class PdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ── Paleta de colores ────────────────────────────────────────────────────
    // Cambiado de Color.BLACK / DKGRAY planos a una paleta más coherente
    private val colorAccent  = Color.rgb(30, 90, 160)   // azul corporativo para títulos
    private val colorHeading = Color.rgb(50, 50, 50)    // casi negro para secciones
    private val colorBody    = Color.rgb(90, 90, 90)    // gris medio para cuerpo
    private val colorMeta    = Color.rgb(130, 130, 130) // gris claro para metadatos
    private val colorLine    = Color.rgb(210, 210, 210) // separador muy sutil

    // ── Tipografía ───────────────────────────────────────────────────────────
    // Se usa Typeface explícito en lugar de isFakeBoldText (mejor renderizado)
    private val paintTitle = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 20f        // era 18f — un poco más de presencia para el título
        color    = colorAccent
        isAntiAlias = true    // suavizado: mejora legibilidad a cualquier escala
    }

    private val paintSection = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 13f        // era 14f — armoniza mejor con el cuerpo
        color    = colorHeading
        isAntiAlias = true
    }

    private val paintBody = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = 10f        // era 11f — más compacto para listas largas
        color    = colorBody
        isAntiAlias = true
    }

    // NUEVO: estilo diferenciado para metadatos (estado, valor, fecha…)
    private val paintMeta = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 9f
        color    = colorMeta
        isAntiAlias = true
    }

    private val paintLine = Paint().apply {
        color       = colorLine
        strokeWidth = 0.8f    // era 1f — línea más fina y elegante
    }

    // ── Dimensiones de página (A4) ───────────────────────────────────────────
    private val pageWidth  = 595
    private val pageHeight = 842

    // ── Márgenes y espaciado ─────────────────────────────────────────────────
    // Margen lateral aumentado para mayor aire visual
    private val marginH      = 48f   // era 40f — horizontal
    private val marginV      = 36f   // nuevo — vertical (top/bottom)
    private val lineHeight   = 16f   // era 18f — más compacto
    private val sectionGap   = 10f   // espacio antes de cada colección
    private val itemIndent   = 14f   // sangría de ítems respecto a la colección
    private val metaIndent   = 22f   // sangría extra para líneas de metadatos

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
        // Línea de acento bajo el título (más gruesa y del color corporativo)
        val accentLine = Paint().apply { color = colorAccent; strokeWidth = 2f }
        canvas.drawLine(marginH, y, pageWidth - marginH, y, accentLine)
        y += 24f  // era 20f — más separación tras la cabecera

        // ── Colecciones ──────────────────────────────────────────────────────
        data.forEach { entry ->

            val c = entry.coleccion
            checkSpace(70f)

            y += sectionGap

            // Nombre de la colección
            canvas.drawText("■  ${c.nombre}", marginH, y, paintSection)
            y += lineHeight + 2f

            // Descripción (si existe) — con sangría de ítems
            c.descripcion?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, marginH + itemIndent, y, paintBody)
                y += lineHeight
            }

            // Resumen de colección en línea de metadatos
            canvas.drawText(
                "Creada: ${dateFormat.format(c.fechaCreacion)}    " +
                        "Items: ${entry.items.size}    " +
                        "Valor total: ${"%.2f".format(entry.items.sumOf { it.valor })} €",
                marginH + itemIndent, y, paintMeta   // era paintBody — diferenciado visualmente
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

                    // Metadatos del ítem en fuente mono (fácil de escanear)
                    canvas.drawText(
                        "Estado: ${item.estado}   " +
                                "Valor: ${"%.2f".format(item.valor)} €   " +
                                "★ ${"%.1f".format(item.calificacion)}   " +
                                "Adq: ${dateFormat.format(item.fechaAdquisicion)}",
                        marginH + metaIndent, y, paintMeta
                    )
                    y += lineHeight

                    // Descripción del ítem (opcional)
                    item.descripcion?.takeIf { it.isNotBlank() }?.let {
                        canvas.drawText(it, marginH + metaIndent, y, paintBody)
                        y += lineHeight
                    }

                    y += 4f
                }
            }

            // Separador de sección
            y += 8f   // era 10f — el espacio extra ya lo aporta sectionGap al inicio
            canvas.drawLine(marginH, y, pageWidth - marginH, y, paintLine)
            y += 12f  // era 14f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "colecciones_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }
}