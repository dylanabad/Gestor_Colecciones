package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase encargada de la exportación de las colecciones del usuario a un archivo PDF.
 * Genera un catálogo visualmente atractivo que incluye:
 * - Una portada personalizada con estadísticas globales.
 * - Un resumen general con tarjetas de datos y un índice.
 * - Páginas individuales por cada colección con sus respectivos ítems.
 *
 * @property context El contexto de la aplicación para acceder a directorios de caché.
 */
class CatalogoPdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Dimensiones de la página (A4 a 72 DPI aprox)
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val contentWidth = pageWidth - margin * 2

    // Colores corporativos y de diseño
    private val colorPrimary = Color.parseColor("#1976D2")
    private val colorPrimaryLight = Color.parseColor("#E3F2FD")
    private val colorDark = Color.parseColor("#1A1A2E")
    private val colorGray = Color.parseColor("#757575")
    private val colorLightGray = Color.parseColor("#F5F5F5")
    private val colorDivider = Color.parseColor("#E0E0E0")
    private val colorWhite = Color.WHITE
    private val colorGold = Color.parseColor("#FFC107")

    // Configuraciones de Paint para distintos elementos visuales
    private val paintCoverBg = Paint().apply { color = colorPrimary }
    private val paintCoverTitle = Paint().apply {
        color = colorWhite; textSize = 32f; isFakeBoldText = true
        isAntiAlias = true
    }
    private val paintCoverSubtitle = Paint().apply {
        color = colorWhite; textSize = 14f; isAntiAlias = true
        alpha = 200
    }
    private val paintCoverDate = Paint().apply {
        color = colorWhite; textSize = 11f; isAntiAlias = true
        alpha = 160
    }
    private val paintColeccionBg = Paint().apply { color = colorPrimaryLight }
    private val paintColeccionTitle = Paint().apply {
        color = colorPrimary; textSize = 18f; isFakeBoldText = true; isAntiAlias = true
    }
    private val paintColeccionMeta = Paint().apply {
        color = colorGray; textSize = 10f; isAntiAlias = true
    }
    private val paintItemTitle = Paint().apply {
        color = colorDark; textSize = 13f; isFakeBoldText = true; isAntiAlias = true
    }
    private val paintItemBody = Paint().apply {
        color = colorGray; textSize = 10f; isAntiAlias = true
    }
    private val paintItemDesc = Paint().apply {
        color = colorGray; textSize = 9f; isAntiAlias = true; textSkewX = -0.15f
    }
    private val paintBadgeBg = Paint().apply { color = colorPrimary; isAntiAlias = true }
    private val paintBadgeText = Paint().apply {
        color = colorWhite; textSize = 9f; isFakeBoldText = true; isAntiAlias = true
    }
    private val paintDivider = Paint().apply { color = colorDivider; strokeWidth = 1f }
    private val paintCardBg = Paint().apply { color = colorWhite; isAntiAlias = true }
    private val paintCardShadow = Paint().apply {
        color = Color.parseColor("#1A000000"); isAntiAlias = true
    }
    private val paintPageNumber = Paint().apply {
        color = colorGray; textSize = 9f; isAntiAlias = true
    }
    private val paintStarFill = Paint().apply { color = colorGold; isAntiAlias = true }
    private val paintStarEmpty = Paint().apply {
        color = colorDivider; isAntiAlias = true
    }
    private val paintImagePlaceholder = Paint().apply { color = colorLightGray }
    private val paintImagePlaceholderText = Paint().apply {
        color = colorGray; textSize = 9f; isAntiAlias = true
    }
    private val paintSummaryBg = Paint().apply { color = colorLightGray }
    private val paintSummaryValue = Paint().apply {
        color = colorPrimary; textSize = 20f; isFakeBoldText = true; isAntiAlias = true
    }
    private val paintSummaryLabel = Paint().apply {
        color = colorGray; textSize = 9f; isAntiAlias = true
    }

    // Estado interno para la generación del documento
    private lateinit var document: PdfDocument
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f

    /**
     * Inicia el proceso de exportación y genera el archivo PDF.
     *
     * @param data Lista de [ColeccionExportData] que contiene las colecciones y sus ítems a exportar.
     * @return Un objeto [File] que apunta al archivo PDF generado en el directorio de caché.
     */
    fun export(data: List<ColeccionExportData>): File {
        document = PdfDocument()

        // 1. Generar la portada del catálogo
        newPage()
        drawCover(data)

        // 2. Generar el resumen general y el índice
        newPage()
        drawSummary(data)

        // 3. Generar las páginas de contenido detallado
        data.forEach { entry ->
            newPage()
            drawColeccionPage(entry)
        }

        // Finalización del documento
        document.finishPage(page)

        val file = File(context.cacheDir, "catalogo_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    /**
     * Finaliza la página actual si existe e inicia una nueva página en el documento.
     * Reinicia la posición vertical [y] al margen superior.
     */
    private fun newPage() {
        if (pageNumber > 0) document.finishPage(page)
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        y = margin
    }

    /**
     * Verifica si hay suficiente espacio vertical en la página actual.
     * Si no hay espacio, dibuja el número de página y salta a una nueva.
     *
     * @param needed El espacio vertical necesario en píxeles.
     */
    private fun checkSpace(needed: Float) {
        if (y + needed > pageHeight - margin - 20f) {
            drawPageNumber()
            newPage()
        }
    }

    /**
     * Dibuja el número de página actual centrado en la parte inferior.
     */
    private fun drawPageNumber() {
        val text = "$pageNumber"
        val x = pageWidth / 2f - paintPageNumber.measureText(text) / 2f
        canvas.drawText(text, x, pageHeight - 16f, paintPageNumber)
    }

    /**
     * Diseña y dibuja la portada del catálogo con elementos gráficos y estadísticas.
     *
     * @param data Datos de las colecciones para extraer estadísticas globales.
     */
    private fun drawCover(data: List<ColeccionExportData>) {
        // Fondo principal
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paintCoverBg)

        // Elementos decorativos (círculos)
        val paintCircle1 = Paint().apply { color = Color.parseColor("#1AFFFFFF"); isAntiAlias = true }
        val paintCircle2 = Paint().apply { color = Color.parseColor("#0DFFFFFF"); isAntiAlias = true }
        canvas.drawCircle(pageWidth.toFloat(), 0f, 220f, paintCircle1)
        canvas.drawCircle(pageWidth.toFloat(), 0f, 160f, paintCircle2)
        canvas.drawCircle(0f, pageHeight.toFloat(), 180f, paintCircle1)

        // Icono representativo
        val iconSize = 64f
        val iconX = margin
        val iconY = pageHeight / 2f - 100f
        val paintIconBg = Paint().apply { color = Color.parseColor("#33FFFFFF"); isAntiAlias = true }
        canvas.drawRoundRect(RectF(iconX, iconY, iconX + iconSize, iconY + iconSize), 16f, 16f, paintIconBg)
        val paintIconText = Paint().apply { color = colorWhite; textSize = 32f; isAntiAlias = true }
        canvas.drawText("📦", iconX + 12f, iconY + 46f, paintIconText)

        // Títulos de la portada
        val titleY = iconY + iconSize + 28f
        canvas.drawText("Gestor de", margin, titleY, paintCoverTitle)
        canvas.drawText("Colecciones", margin, titleY + 40f, paintCoverTitle)

        // Línea de acento y subtítulo
        val paintAccentLine = Paint().apply { color = colorWhite; strokeWidth = 4f; alpha = 180 }
        canvas.drawLine(margin, titleY + 52f, margin + 60f, titleY + 52f, paintAccentLine)
        canvas.drawText("Catálogo completo de colecciones", margin, titleY + 72f, paintCoverSubtitle)

        // Estadísticas rápidas en portada
        val statsY = titleY + 110f
        val totalItems = data.sumOf { it.items.size }
        val totalValor = data.sumOf { entry -> entry.items.sumOf { it.valor } }

        listOf(
            "📚 ${data.size} colecciones",
            "🗂 $totalItems items",
            "💰 ${"%.2f".format(totalValor)} €"
        ).forEachIndexed { i, text ->
            canvas.drawText(text, margin, statsY + i * 20f, paintCoverSubtitle)
        }

        // Fecha de generación
        val fechaText = "Generado el ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
        canvas.drawText(fechaText, margin, pageHeight - 40f, paintCoverDate)
    }

    /**
     * Dibuja la página de resumen que incluye tarjetas con métricas clave
     * y un índice detallado de todas las colecciones.
     *
     * @param data Datos de las colecciones.
     */
    private fun drawSummary(data: List<ColeccionExportData>) {
        y = margin
        drawSectionTitle("Resumen general")
        y += 8f

        val totalItems = data.sumOf { it.items.size }
        val totalValor = data.sumOf { entry -> entry.items.sumOf { it.valor } }
        val itemMasValioso = data.flatMap { it.items }.maxByOrNull { it.valor }

        // Dibujar tarjetas de métricas
        val cardW = (contentWidth - 12f) / 3f
        val cardH = 72f
        val cards = listOf(
            Triple("${data.size}", "Colecciones", "📚"),
            Triple("$totalItems", "Items totales", "🗂"),
            Triple("${"%.0f".format(totalValor)} €", "Valor total", "💰")
        )

        cards.forEachIndexed { i, (value, label, icon) ->
            val cx = margin + i * (cardW + 6f)
            canvas.drawRoundRect(RectF(cx, y, cx + cardW, y + cardH), 12f, 12f, paintCardShadow.apply { alpha = 20 })
            canvas.drawRoundRect(RectF(cx, y, cx + cardW, y + cardH), 12f, 12f, paintCardBg)
            canvas.drawRoundRect(RectF(cx, y, cx + cardW, y + cardH), 12f, 12f,
                Paint().apply { color = colorDivider; style = Paint.Style.STROKE; strokeWidth = 1f })

            canvas.drawText(icon, cx + 10f, y + 24f, Paint().apply { textSize = 16f; isAntiAlias = true })
            canvas.drawText(value, cx + 10f, y + 46f, paintSummaryValue.apply { textSize = 16f })
            canvas.drawText(label, cx + 10f, y + 60f, paintSummaryLabel)
        }

        y += cardH + 20f

        // Dibujar sección de ítem destacado
        itemMasValioso?.let { item ->
            canvas.drawRoundRect(RectF(margin, y, margin + contentWidth, y + 48f), 10f, 10f, paintColeccionBg)
            canvas.drawText("⭐  Item más valioso", margin + 12f, y + 18f, paintColeccionMeta.apply { isFakeBoldText = true })
            canvas.drawText("${item.titulo}  —  ${"%.2f".format(item.valor)} €  |  ★ ${"%.1f".format(item.calificacion)}",
                margin + 12f, y + 36f, paintColeccionMeta)
            y += 60f
        }

        y += 8f
        drawSectionTitle("Índice de colecciones")
        y += 8f

        // Listado tipo índice
        data.forEachIndexed { index, entry ->
            checkSpace(28f)
            val rowBg = if (index % 2 == 0) colorWhite else Color.parseColor("#FAFAFA")
            canvas.drawRect(margin, y - 14f, margin + contentWidth, y + 8f, Paint().apply { color = rowBg })
            canvas.drawText("${index + 1}.", margin + 4f, y, paintItemBody)
            canvas.drawText(entry.coleccion.nombre, margin + 24f, y, paintItemTitle.apply { textSize = 11f })
            val meta = "${entry.items.size} items  |  ${"%.2f".format(entry.items.sumOf { it.valor })} €"
            canvas.drawText(meta, margin + contentWidth - paintItemBody.measureText(meta), y, paintItemBody)
            y += 22f
        }

        drawPageNumber()
    }

    /**
     * Dibuja una página dedicada a una única colección, mostrando su cabecera
     * y una cuadrícula de dos columnas con sus ítems.
     *
     * @param entry Datos de la colección específica.
     */
    private fun drawColeccionPage(entry: ColeccionExportData) {
        y = margin
        val headerH = 64f

        // Cabecera de la colección
        canvas.drawRoundRect(RectF(margin, y, margin + contentWidth, y + headerH), 12f, 12f, paintColeccionBg)
        val colImgSize = 48f
        var textStartX = margin + 14f

        entry.coleccion.imagenPath?.let { path ->
            loadBitmap(path)?.let { bmp ->
                val scaled = scaleBitmap(bmp, colImgSize.toInt(), colImgSize.toInt())
                val imgX = margin + 10f
                val imgY = y + (headerH - colImgSize) / 2f
                canvas.drawRoundRect(RectF(imgX, imgY, imgX + colImgSize, imgY + colImgSize), 8f, 8f, Paint().apply { color = colorDivider })
                canvas.drawBitmap(scaled, imgX, imgY, null)
                textStartX = imgX + colImgSize + 10f
            }
        }

        canvas.drawText(entry.coleccion.nombre, textStartX, y + 26f, paintColeccionTitle)
        canvas.drawText(
            "${entry.items.size} items  ·  ${"%.2f".format(entry.items.sumOf { it.valor })} €  ·  Creada: ${dateFormat.format(entry.coleccion.fechaCreacion)}",
            textStartX, y + 44f, paintColeccionMeta
        )

        y += headerH + 12f

        // Descripción de la colección
        entry.coleccion.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
            canvas.drawText(desc, margin, y, paintItemDesc)
            y += 16f
        }

        y += 4f

        if (entry.items.isEmpty()) {
            canvas.drawText("Esta colección no tiene items.", margin, y, paintItemBody)
            y += 20f
        } else {
            // Cuadrícula de ítems (2 columnas)
            val colGap = 10f
            val itemCardW = (contentWidth - colGap) / 2f
            val imgH = 90f
            val cardPad = 10f

            entry.items.chunked(2).forEach { rowItems ->
                val cardH = imgH + 72f
                checkSpace(cardH + 12f)

                rowItems.forEachIndexed { col, item ->
                    val cx = margin + col * (itemCardW + colGap)
                    val cy = y

                    // Sombra y fondo de tarjeta
                    canvas.drawRoundRect(RectF(cx + 2f, cy + 2f, cx + itemCardW + 2f, cy + cardH + 2f), 10f, 10f, paintCardShadow.apply { alpha = 15 })
                    canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + cardH), 10f, 10f, paintCardBg)
                    canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + cardH), 10f, 10f,
                        Paint().apply { color = colorDivider; style = Paint.Style.STROKE; strokeWidth = 0.8f })

                    // Imagen del ítem
                    val imgLoaded = item.imagenPath?.let { path ->
                        loadBitmap(path)?.let { bmp -> scaleBitmap(bmp, itemCardW.toInt(), imgH.toInt()) }
                    }

                    if (imgLoaded != null) {
                        val imgRect = RectF(cx, cy, cx + itemCardW, cy + imgH)
                        canvas.save()
                        canvas.clipRect(imgRect)
                        canvas.drawBitmap(imgLoaded, cx, cy, null)
                        canvas.restore()
                    } else {
                        canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + imgH), 10f, 10f, paintImagePlaceholder)
                        val noImgText = "Sin imagen"
                        val tx = cx + itemCardW / 2f - paintImagePlaceholderText.measureText(noImgText) / 2f
                        canvas.drawText(noImgText, tx, cy + imgH / 2f + 4f, paintImagePlaceholderText)
                    }

                    // Etiqueta de estado
                    val badgeText = item.estado
                    val badgeW = paintBadgeText.measureText(badgeText) + 12f
                    val badgeX = cx + itemCardW - badgeW - 6f
                    val badgeY = cy + imgH - 20f
                    canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + 14f), 7f, 7f, paintBadgeBg)
                    canvas.drawText(badgeText, badgeX + 6f, badgeY + 10f, paintBadgeText)

                    // Información del ítem
                    val textY = cy + imgH + cardPad + 12f
                    var titleText = item.titulo
                    val maxTitleW = itemCardW - cardPad * 2
                    while (paintItemTitle.measureText(titleText) > maxTitleW && titleText.length > 4) {
                        titleText = titleText.dropLast(1)
                    }
                    if (titleText != item.titulo) titleText += "…"

                    canvas.drawText(titleText, cx + cardPad, textY, paintItemTitle.apply { textSize = 11f })
                    canvas.drawText("${"%.2f".format(item.valor)} €", cx + cardPad, textY + 16f, paintColeccionTitle.apply { textSize = 13f })
                    drawStars(cx + cardPad, textY + 32f, item.calificacion)
                    canvas.drawText("Adq: ${dateFormat.format(item.fechaAdquisicion)}", cx + cardPad, textY + 50f, paintItemBody.apply { textSize = 9f })

                    // Descripción corta truncada
                    item.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
                        val maxDescW = itemCardW - cardPad * 2
                        var descText = desc
                        while (paintItemDesc.measureText(descText) > maxDescW && descText.length > 4) {
                            descText = descText.dropLast(1)
                        }
                        if (descText != desc) descText += "…"
                        canvas.drawText(descText, cx + cardPad, textY + 62f, paintItemDesc)
                    }
                }
                y += cardH + 12f
            }
        }

        // Línea decorativa final
        canvas.drawLine(margin, y, margin + contentWidth, y, paintDivider)
        y += 8f
        drawPageNumber()
    }

    /**
     * Dibuja un título de sección con una línea de acento debajo.
     *
     * @param title El texto del título a dibujar.
     */
    private fun drawSectionTitle(title: String) {
        canvas.drawText(title, margin, y, paintColeccionTitle.apply { textSize = 16f })
        y += 6f
        canvas.drawLine(margin, y, margin + 80f, y, Paint().apply { color = colorPrimary; strokeWidth = 2f })
        y += 14f
    }

    /**
     * Dibuja una fila de 5 estrellas representando la calificación del ítem.
     *
     * @param x Posición horizontal de inicio.
     * @param y Posición vertical.
     * @param rating Calificación (0-5).
     */
    private fun drawStars(x: Float, y: Float, rating: Float) {
        val starSize = 10f
        val gap = 2f
        for (i in 1..5) {
            val paint = if (i <= rating) paintStarFill else paintStarEmpty
            canvas.drawRoundRect(RectF(x + (i - 1) * (starSize + gap), y - starSize + 2f,
                    x + (i - 1) * (starSize + gap) + starSize, y + 2f), 3f, 3f, paint)
        }
    }

    /**
     * Intenta cargar un bitmap desde una ruta local.
     *
     * @param path Ruta absoluta del archivo de imagen.
     * @return El [Bitmap] cargado o null si ocurre un error.
     */
    private fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Escala y recorta un bitmap para que encaje perfectamente en las dimensiones objetivo,
     * manteniendo la relación de aspecto y centrando el contenido.
     *
     * @param bitmap El bitmap original.
     * @param targetW Ancho objetivo.
     * @param targetH Alto objetivo.
     * @return Un nuevo [Bitmap] escalado y recortado.
     */
    private fun scaleBitmap(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        val scaleW = targetW / srcW
        val scaleH = targetH / srcH
        val scale = maxOf(scaleW, scaleH)

        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)

        val offsetX = (scaledW - targetW) / 2
        val offsetY = (scaledH - targetH) / 2

        return Bitmap.createBitmap(scaled, offsetX, offsetY, targetW, targetH)
    }
}
