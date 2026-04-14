package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.gestor_colecciones.repository.ColeccionExportData
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CatalogoPdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ── Página ───────────────────────────────────────────────────────────────
    private val pageWidth    = 595
    private val pageHeight   = 842
    private val margin       = 40f          // era 36f — más aire lateral
    private val contentWidth = pageWidth - margin * 2

    // ── Paleta ───────────────────────────────────────────────────────────────
    // Se sustituye el azul Material puro por uno más sobrio y profesional
    private val colorPrimary      = Color.parseColor("#1A3A5C")  // era #1976D2 — azul marino
    private val colorPrimaryLight = Color.parseColor("#EBF2FA")  // era #E3F2FD — más cálido
    private val colorAccent       = Color.parseColor("#2E86C1")  // nuevo — versión media del primario
    private val colorDark         = Color.parseColor("#1A1A2E")
    private val colorGray         = Color.parseColor("#6B6B6B")  // era #757575 — ligeramente más oscuro
    private val colorLightGray    = Color.parseColor("#F7F7F7")  // era #F5F5F5
    private val colorDivider      = Color.parseColor("#D8D8D8")  // era #E0E0E0 — más visible
    private val colorWhite        = Color.WHITE
    private val colorGold         = Color.parseColor("#E8A800")  // era #FFC107 — oro más apagado

    // ── Tipografía ───────────────────────────────────────────────────────────
    // Se elimina isFakeBoldText y se usan Typeface reales en todos los Paint
    private val bold    = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val normal  = Typeface.DEFAULT
    private val italic  = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    private val mono    = Typeface.MONOSPACE

    // Portada
    private val paintCoverBg = Paint().apply { color = colorPrimary }
    private val paintCoverTitle = Paint().apply {
        typeface = bold; color = colorWhite; textSize = 30f  // era 32f
        isAntiAlias = true
    }
    private val paintCoverSubtitle = Paint().apply {
        typeface = normal; color = colorWhite; textSize = 13f  // era 14f
        isAntiAlias = true; alpha = 210
    }
    private val paintCoverDate = Paint().apply {
        typeface = mono; color = colorWhite; textSize = 10f    // era 11f
        isAntiAlias = true; alpha = 170
    }

    // Cabecera de colección
    private val paintColeccionBg = Paint().apply { color = colorPrimaryLight }
    private val paintColeccionTitle = Paint().apply {
        typeface = bold; color = colorPrimary; textSize = 17f  // era 18f
        isAntiAlias = true
    }
    private val paintColeccionMeta = Paint().apply {
        typeface = mono; color = colorGray; textSize = 9f      // era 10f — mono para datos
        isAntiAlias = true
    }

    // Ítems
    private val paintItemTitle = Paint().apply {
        typeface = bold; color = colorDark; textSize = 12f     // era 13f
        isAntiAlias = true
    }
    private val paintItemBody = Paint().apply {
        typeface = normal; color = colorGray; textSize = 9f    // era 10f
        isAntiAlias = true
    }
    private val paintItemDesc = Paint().apply {
        typeface = italic; color = colorGray; textSize = 8f    // era 9f
        isAntiAlias = true
        // Eliminado textSkewX — el italic del Typeface ya aplica inclinación correcta
    }

    // Badge de estado
    private val paintBadgeBg = Paint().apply { color = colorAccent; isAntiAlias = true }  // era colorPrimary
    private val paintBadgeText = Paint().apply {
        typeface = bold; color = colorWhite; textSize = 8f     // era 9f
        isAntiAlias = true
    }

    // Separadores y fondos
    private val paintDivider   = Paint().apply { color = colorDivider; strokeWidth = 0.8f }  // era 1f
    private val paintCardBg    = Paint().apply { color = colorWhite; isAntiAlias = true }
    private val paintCardShadow = Paint().apply {
        color = Color.parseColor("#18000000"); isAntiAlias = true  // era #1A000000 — sombra más sutil
    }

    // Número de página
    private val paintPageNumber = Paint().apply {
        typeface = mono; color = colorGray; textSize = 8f      // era 9f
        isAntiAlias = true
    }

    // Estrellas
    private val paintStarFill  = Paint().apply { color = colorGold; isAntiAlias = true }
    private val paintStarEmpty = Paint().apply { color = colorDivider; isAntiAlias = true }

    // Placeholder imagen
    private val paintImagePlaceholder     = Paint().apply { color = colorLightGray }
    private val paintImagePlaceholderText = Paint().apply {
        typeface = italic; color = colorGray; textSize = 9f; isAntiAlias = true
    }

    // Resumen
    private val paintSummaryBg = Paint().apply { color = colorLightGray }
    private val paintSummaryValue = Paint().apply {
        typeface = bold; color = colorPrimary; textSize = 18f  // era 20f
        isAntiAlias = true
    }
    private val paintSummaryLabel = Paint().apply {
        typeface = normal; color = colorGray; textSize = 9f
        isAntiAlias = true
    }

    // ── Estado interno ───────────────────────────────────────────────────────
    private lateinit var document: PdfDocument
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f

    fun export(data: List<ColeccionExportData>): File {
        document = PdfDocument()
        newPage(); drawCover(data)
        newPage(); drawSummary(data)
        data.forEach { entry -> newPage(); drawColeccionPage(entry) }
        document.finishPage(page)
        val file = File(context.cacheDir, "catalogo_export.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun newPage() {
        if (pageNumber > 0) document.finishPage(page)
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page   = document.startPage(pageInfo)
        canvas = page.canvas
        y      = margin
    }

    private fun checkSpace(needed: Float) {
        if (y + needed > pageHeight - margin - 20f) { drawPageNumber(); newPage() }
    }

    private fun drawPageNumber() {
        val text = "$pageNumber"
        val x = pageWidth / 2f - paintPageNumber.measureText(text) / 2f
        canvas.drawText(text, x, pageHeight - 16f, paintPageNumber)
    }

    private fun drawCover(data: List<ColeccionExportData>) {
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paintCoverBg)

        // Círculos decorativos — opacidad reducida para no competir con el texto
        val c1 = Paint().apply { color = Color.parseColor("#14FFFFFF"); isAntiAlias = true }
        val c2 = Paint().apply { color = Color.parseColor("#0AFFFFFF"); isAntiAlias = true }
        canvas.drawCircle(pageWidth.toFloat(), 0f, 200f, c1)
        canvas.drawCircle(pageWidth.toFloat(), 0f, 140f, c2)
        canvas.drawCircle(0f, pageHeight.toFloat(), 160f, c1)

        // Icono
        val iconSize = 60f          // era 64f
        val iconX    = margin
        val iconY    = pageHeight / 2f - 110f
        val paintIconBg = Paint().apply { color = Color.parseColor("#2AFFFFFF"); isAntiAlias = true }
        canvas.drawRoundRect(RectF(iconX, iconY, iconX + iconSize, iconY + iconSize), 14f, 14f, paintIconBg)
        canvas.drawText("📦", iconX + 10f, iconY + 44f,
            Paint().apply { color = colorWhite; textSize = 30f; isAntiAlias = true })

        // Títulos
        val titleY = iconY + iconSize + 30f
        canvas.drawText("Gestor de", margin, titleY, paintCoverTitle)
        canvas.drawText("Colecciones", margin, titleY + 38f, paintCoverTitle)

        // Línea de acento — más corta y fina para mayor elegancia
        val accentLine = Paint().apply { color = colorWhite; strokeWidth = 3f; alpha = 160 }
        canvas.drawLine(margin, titleY + 50f, margin + 50f, titleY + 50f, accentLine)

        canvas.drawText("Catálogo completo de colecciones", margin, titleY + 68f, paintCoverSubtitle)

        // Estadísticas
        val totalItems = data.sumOf { it.items.size }
        val totalValor = data.sumOf { entry -> entry.items.sumOf { it.valor } }
        val statsY     = titleY + 106f  // era +110f
        listOf(
            "📚 ${data.size} colecciones",
            "🗂 $totalItems items",
            "💰 ${"%.2f".format(totalValor)} €"
        ).forEachIndexed { i, text ->
            canvas.drawText(text, margin, statsY + i * 22f, paintCoverSubtitle)  // era *20f
        }

        val fechaText = "Generado el ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
        canvas.drawText(fechaText, margin, pageHeight - 44f, paintCoverDate)  // era -40f
    }

    private fun drawSummary(data: List<ColeccionExportData>) {
        y = margin
        drawSectionTitle("Resumen general")
        y += 8f

        val totalItems    = data.sumOf { it.items.size }
        val totalValor    = data.sumOf { entry -> entry.items.sumOf { it.valor } }
        val itemMasValioso = data.flatMap { it.items }.maxByOrNull { it.valor }

        // Tarjetas de métricas
        val cardW = (contentWidth - 12f) / 3f
        val cardH = 68f  // era 72f — más compactas
        listOf(
            Triple("${data.size}", "Colecciones", "📚"),
            Triple("$totalItems", "Items totales", "🗂"),
            Triple("${"%.0f".format(totalValor)} €", "Valor total", "💰")
        ).forEachIndexed { i, (value, label, icon) ->
            val cx = margin + i * (cardW + 6f)
            canvas.drawRoundRect(RectF(cx + 1f, cy(1f), cx + cardW + 1f, cy(cardH + 1f)), 10f, 10f,
                paintCardShadow.apply { alpha = 18 })
            canvas.drawRoundRect(RectF(cx, y, cx + cardW, y + cardH), 10f, 10f, paintCardBg)
            canvas.drawRoundRect(RectF(cx, y, cx + cardW, y + cardH), 10f, 10f,
                Paint().apply { color = colorDivider; style = Paint.Style.STROKE; strokeWidth = 0.8f })

            canvas.drawText(icon, cx + 10f, y + 22f,
                Paint().apply { textSize = 15f; isAntiAlias = true })
            canvas.drawText(value, cx + 10f, y + 44f, paintSummaryValue.apply { textSize = 15f })
            canvas.drawText(label, cx + 10f, y + 58f, paintSummaryLabel)
        }

        y += cardH + 18f  // era +20f

        // Ítem destacado
        itemMasValioso?.let { item ->
            canvas.drawRoundRect(RectF(margin, y, margin + contentWidth, y + 46f), 8f, 8f, paintColeccionBg)
            canvas.drawText("⭐  Item más valioso", margin + 12f, y + 17f,
                paintColeccionMeta.apply { typeface = bold })
            canvas.drawText(
                "${item.titulo}  —  ${"%.2f".format(item.valor)} €  |  ★ ${"%.1f".format(item.calificacion)}",
                margin + 12f, y + 34f, paintColeccionMeta.apply { typeface = mono }
            )
            y += 58f  // era 60f
        }

        y += 8f
        drawSectionTitle("Índice de colecciones")
        y += 8f

        data.forEachIndexed { index, entry ->
            checkSpace(26f)  // era 28f
            val rowBg = if (index % 2 == 0) colorWhite else Color.parseColor("#F9F9F9")  // era #FAFAFA
            canvas.drawRect(margin, y - 14f, margin + contentWidth, y + 8f,
                Paint().apply { color = rowBg })
            canvas.drawText("${index + 1}.", margin + 4f, y, paintItemBody)
            canvas.drawText(entry.coleccion.nombre, margin + 24f, y,
                paintItemTitle.apply { textSize = 11f })
            val meta = "${entry.items.size} items  |  ${"%.2f".format(entry.items.sumOf { it.valor })} €"
            canvas.drawText(meta,
                margin + contentWidth - paintItemBody.measureText(meta), y, paintItemBody)
            y += 22f
        }

        drawPageNumber()
    }

    private fun drawColeccionPage(entry: ColeccionExportData) {
        y = margin
        val headerH = 60f  // era 64f — cabecera más compacta

        canvas.drawRoundRect(RectF(margin, y, margin + contentWidth, y + headerH), 10f, 10f, paintColeccionBg)

        val colImgSize  = 44f   // era 48f
        var textStartX  = margin + 14f

        entry.coleccion.imagenPath?.let { path ->
            loadBitmap(path)?.let { bmp ->
                val scaled = scaleBitmap(bmp, colImgSize.toInt(), colImgSize.toInt())
                val imgX   = margin + 10f
                val imgY   = y + (headerH - colImgSize) / 2f
                canvas.drawRoundRect(RectF(imgX, imgY, imgX + colImgSize, imgY + colImgSize),
                    6f, 6f, Paint().apply { color = colorDivider })
                canvas.drawBitmap(scaled, imgX, imgY, null)
                textStartX = imgX + colImgSize + 10f
            }
        }

        canvas.drawText(entry.coleccion.nombre, textStartX, y + 24f, paintColeccionTitle)
        canvas.drawText(
            "${entry.items.size} items  ·  ${"%.2f".format(entry.items.sumOf { it.valor })} €  ·  Creada: ${dateFormat.format(entry.coleccion.fechaCreacion)}",
            textStartX, y + 40f, paintColeccionMeta
        )

        y += headerH + 14f  // era +12f — algo más de respiro bajo la cabecera

        entry.coleccion.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
            canvas.drawText(desc, margin, y, paintItemDesc)
            y += 18f  // era 16f
        }

        y += 6f  // era 4f

        if (entry.items.isEmpty()) {
            canvas.drawText("Esta colección no tiene items.", margin, y, paintItemBody)
            y += 20f
        } else {
            val colGap   = 12f   // era 10f — más respiro entre columnas
            val itemCardW = (contentWidth - colGap) / 2f
            // En drawColeccionPage — dentro del bloque else de items

            val imgH     = 86f
            val cardPad  = 10f
            val cardH    = imgH + 88f   // era +70f → los textos llegan hasta textY+60f (108+60=168) + padding inferior

            entry.items.chunked(2).forEach { rowItems ->
                checkSpace(cardH + 14f)

                rowItems.forEachIndexed { col, item ->
                    val cx = margin + col * (itemCardW + colGap)
                    val cy = y

                    // Sombra y tarjeta
                    canvas.drawRoundRect(RectF(cx + 2f, cy + 2f, cx + itemCardW + 2f, cy + cardH + 2f),
                        8f, 8f, paintCardShadow.apply { alpha = 12 })  // era alpha=15
                    canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + cardH),
                        8f, 8f, paintCardBg)  // radio era 10f — más sutil
                    canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + cardH),
                        8f, 8f, Paint().apply { color = colorDivider; style = Paint.Style.STROKE; strokeWidth = 0.7f })

                    // Imagen o placeholder
                    val imgLoaded = item.imagenPath?.let { path ->
                        loadBitmap(path)?.let { bmp -> scaleBitmap(bmp, itemCardW.toInt(), imgH.toInt()) }
                    }
                    if (imgLoaded != null) {
                        val imgRect = RectF(cx, cy, cx + itemCardW, cy + imgH)
                        canvas.save(); canvas.clipRect(imgRect)
                        canvas.drawBitmap(imgLoaded, cx, cy, null)
                        canvas.restore()
                    } else {
                        canvas.drawRoundRect(RectF(cx, cy, cx + itemCardW, cy + imgH),
                            8f, 8f, paintImagePlaceholder)
                        val noImgText = "Sin imagen"
                        val tx = cx + itemCardW / 2f - paintImagePlaceholderText.measureText(noImgText) / 2f
                        canvas.drawText(noImgText, tx, cy + imgH / 2f + 4f, paintImagePlaceholderText)
                    }

                    // Badge de estado — separado del borde para no quedar cortado
                    val badgeText = item.estado
                    val badgeW    = paintBadgeText.measureText(badgeText) + 14f  // era +12f
                    val badgeX    = cx + itemCardW - badgeW - 8f                 // era -6f
                    val badgeY    = cy + imgH - 22f                              // era -20f
                    canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + 14f),
                        7f, 7f, paintBadgeBg)
                    canvas.drawText(badgeText, badgeX + 7f, badgeY + 10f, paintBadgeText)

                    // Datos del ítem
                    val textY = cy + imgH + cardPad + 12f

                    var titleText = item.titulo
                    val maxTitleW = itemCardW - cardPad * 2
                    while (paintItemTitle.measureText(titleText) > maxTitleW && titleText.length > 4)
                        titleText = titleText.dropLast(1)
                    if (titleText != item.titulo) titleText += "…"

                    canvas.drawText(titleText, cx + cardPad, textY,
                        paintItemTitle.apply { textSize = 11f })
                    canvas.drawText("${"%.2f".format(item.valor)} €", cx + cardPad, textY + 15f,  // era +16f
                        paintColeccionTitle.apply { textSize = 12f })                              // era 13f
                    drawStars(cx + cardPad, textY + 30f, item.calificacion)                        // era +32f
                    canvas.drawText("Adq: ${dateFormat.format(item.fechaAdquisicion)}",
                        cx + cardPad, textY + 48f,                                                // era +50f
                        paintItemBody.apply { textSize = 9f })

                    item.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
                        val maxDescW = itemCardW - cardPad * 2
                        var descText = desc
                        while (paintItemDesc.measureText(descText) > maxDescW && descText.length > 4)
                            descText = descText.dropLast(1)
                        if (descText != desc) descText += "…"
                        canvas.drawText(descText, cx + cardPad, textY + 60f, paintItemDesc)  // era +62f
                    }
                }
                y += cardH + 14f  // era +12f
            }
        }

        canvas.drawLine(margin, y, margin + contentWidth, y, paintDivider)
        y += 10f  // era 8f
        drawPageNumber()
    }

    private fun drawSectionTitle(title: String) {
        canvas.drawText(title, margin, y,
            paintColeccionTitle.apply { textSize = 15f })  // era 16f
        y += 6f
        // Línea de acento ligeramente más larga
        canvas.drawLine(margin, y, margin + 70f, y,       // era +80f
            Paint().apply { color = colorAccent; strokeWidth = 2f })  // era colorPrimary
        y += 16f  // era 14f
    }

    private fun drawStars(x: Float, y: Float, rating: Float) {
        val starSize = 9f
        val gap      = 3f

        for (i in 1..5) {
            val paint  = if (i <= rating) paintStarFill else paintStarEmpty
            val cx     = x + (i - 1) * (starSize + gap) + starSize / 2f
            val cy     = y - starSize / 2f
            canvas.drawPath(starPath(cx, cy, starSize / 2f), paint)
        }
    }

    /**
     * Genera el Path de una estrella de 5 puntas centrada en (cx, cy).
     * @param outerR Radio exterior (puntas).
     */
    private fun starPath(cx: Float, cy: Float, outerR: Float): android.graphics.Path {
        val innerR  = outerR * 0.42f   // proporción clásica punta/valle de estrella
        val path    = android.graphics.Path()
        val totalPoints = 10           // 5 puntas + 5 valles

        for (i in 0 until totalPoints) {
            val angle  = Math.PI / 5 * i - Math.PI / 2   // empieza en la punta superior
            val r      = if (i % 2 == 0) outerR else innerR
            val px     = cx + (r * Math.cos(angle)).toFloat()
            val py     = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        path.close()
        return path
    }

    // Helper para calcular y+offset sin alterar y
    private fun cy(offset: Float) = y + offset

    private fun loadBitmap(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) { null }

    private fun scaleBitmap(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val scale   = maxOf(targetW / bitmap.width.toFloat(), targetH / bitmap.height.toFloat())
        val scaledW = (bitmap.width  * scale).toInt()
        val scaledH = (bitmap.height * scale).toInt()
        val scaled  = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        return Bitmap.createBitmap(scaled,
            (scaledW - targetW) / 2, (scaledH - targetH) / 2, targetW, targetH)
    }
}