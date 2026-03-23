package com.example.gestor_colecciones.export

import android.content.Context
import android.graphics.*
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import java.io.File
import kotlin.random.Random

class TarjetaColeccionGenerator(private val context: Context) {

    private val size = 1080
    private val padding = 48f

    // ── Paleta ────────────────────────────────────────────────────────────────
    private val colorBg1 = Color.parseColor("#0A0E1A")
    private val colorBg2 = Color.parseColor("#0D1B2A")
    private val colorAccent1 = Color.parseColor("#6C63FF")
    private val colorAccent2 = Color.parseColor("#48CAE4")
    private val colorAccent3 = Color.parseColor("#FF6B9D")
    private val colorGold = Color.parseColor("#FFD166")
    private val colorWhite = Color.WHITE
    private val colorWhite60 = Color.parseColor("#99FFFFFF")
    private val colorWhite30 = Color.parseColor("#4DFFFFFF")
    private val colorCardBg = Color.parseColor("#1A1F35")
    private val colorCardBorder = Color.parseColor("#2A3050")

    // Colores de acento por posición en el ranking
    private val positionAccents = listOf(
        Color.parseColor("#FFD166"), // 1° dorado
        Color.parseColor("#C0C0C0"), // 2° plata
        Color.parseColor("#CD7F32"), // 3° bronce
        Color.parseColor("#6C63FF")  // 4° morado
    )

    fun generate(coleccion: Coleccion, items: List<Item>): File {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawNoise(canvas)
        drawDecorativeElements(canvas)
        drawHeader(canvas, coleccion, items)
        drawItemsGrid(canvas, items.take(4))
        drawFooter(canvas, coleccion, items)

        val file = File(context.cacheDir, "tarjeta_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    // ── Fondo ─────────────────────────────────────────────────────────────────

    private fun drawBackground(canvas: Canvas) {
        val bgGradient = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            colorBg1, colorBg2, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(),
            Paint().apply { shader = bgGradient })
    }

    // ── Textura de ruido ──────────────────────────────────────────────────────

    private fun drawNoise(canvas: Canvas) {
        val noiseBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = Random(42)
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val v = random.nextInt(256)
            val alpha = random.nextInt(18) + 4 // entre 4 y 22 de opacidad
            pixels[i] = Color.argb(alpha, v, v, v)
        }
        noiseBitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        canvas.drawBitmap(noiseBitmap, 0f, 0f, Paint().apply { alpha = 180 })
        noiseBitmap.recycle()
    }

    private fun drawDecorativeElements(canvas: Canvas) {
        val paintBlob1 = Paint().apply {
            color = colorAccent1; alpha = 40; isAntiAlias = true
            maskFilter = BlurMaskFilter(280f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(size.toFloat(), 0f, 420f, paintBlob1)

        val paintBlob2 = Paint().apply {
            color = colorAccent2; alpha = 30; isAntiAlias = true
            maskFilter = BlurMaskFilter(240f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(0f, size.toFloat(), 360f, paintBlob2)

        val paintBlob3 = Paint().apply {
            color = colorAccent3; alpha = 20; isAntiAlias = true
            maskFilter = BlurMaskFilter(200f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(size.toFloat(), size / 2f, 260f, paintBlob3)

        val lineGradient = LinearGradient(
            padding, 0f, size - padding, 0f,
            intArrayOf(Color.TRANSPARENT, colorAccent1, colorAccent2, Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(padding, size * 0.265f, size - padding, size * 0.265f + 2f,
            Paint().apply { shader = lineGradient })
    }

    // ── Cabecera ──────────────────────────────────────────────────────────────

    private fun drawHeader(canvas: Canvas, coleccion: Coleccion, items: List<Item>) {
        val headerTop = padding + 20f
        var textStartX = padding

        coleccion.imagenPath?.let { path ->
            loadAndCropBitmap(path, 110, 110)?.let { bmp ->
                val imgCx = padding + 55f
                val imgCy = headerTop + 55f

                val ringGradient = SweepGradient(imgCx, imgCy,
                    intArrayOf(colorAccent1, colorAccent2, colorAccent3, colorAccent1), null)
                canvas.drawCircle(imgCx, imgCy, 60f,
                    Paint().apply {
                        shader = ringGradient; style = Paint.Style.STROKE
                        strokeWidth = 4f; isAntiAlias = true
                    })

                val circlePaint = Paint().apply {
                    shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    isAntiAlias = true
                }
                canvas.drawCircle(imgCx, imgCy, 53f, circlePaint)
                textStartX = padding + 130f
            }
        }

        val tagPaint = Paint().apply {
            color = colorAccent2; textSize = 22f; isFakeBoldText = true
            isAntiAlias = true; letterSpacing = 0.2f
        }
        canvas.drawText("COLECCIÓN", textStartX, headerTop + 28f, tagPaint)

        val nombrePaint = Paint().apply {
            color = colorWhite; textSize = 68f; isFakeBoldText = true; isAntiAlias = true
        }
        var nombre = coleccion.nombre
        while (nombrePaint.measureText(nombre) > size - textStartX - padding && nombre.length > 3)
            nombre = nombre.dropLast(1)
        if (nombre != coleccion.nombre) nombre += "…"
        canvas.drawText(nombre, textStartX, headerTop + 88f, nombrePaint)

        val accentGradient = LinearGradient(
            textStartX, 0f, textStartX + 120f, 0f,
            colorAccent1, colorAccent2, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            RectF(textStartX, headerTop + 98f, textStartX + 120f, headerTop + 104f),
            3f, 3f, Paint().apply { shader = accentGradient }
        )
    }

    // ── Grid de items ─────────────────────────────────────────────────────────

    private fun drawItemsGrid(canvas: Canvas, items: List<Item>) {
        if (items.isEmpty()) return

        val gridTop = size * 0.285f
        val gridBottom = size * 0.845f
        val gridH = gridBottom - gridTop
        val gap = 14f
        val cellW = (size - padding * 2 - gap) / 2f
        val cellH = (gridH - gap) / 2f
        val imgRatio = 0.56f
        val accentBarW = 5f

        items.forEachIndexed { index, item ->
            val col = index % 2
            val row = index / 2
            val cx = padding + col * (cellW + gap)
            val cy = gridTop + row * (cellH + gap)
            val accentColor = positionAccents[index]

            // Sombra
            canvas.drawRoundRect(
                RectF(cx + 4f, cy + 4f, cx + cellW + 4f, cy + cellH + 4f),
                20f, 20f,
                Paint().apply {
                    color = Color.parseColor("#40000000"); isAntiAlias = true
                    maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                }
            )

            // Fondo tarjeta
            canvas.drawRoundRect(
                RectF(cx, cy, cx + cellW, cy + cellH), 20f, 20f,
                Paint().apply { color = colorCardBg; isAntiAlias = true }
            )

            // ── Línea de acento izquierda según posición ──────────────────────
            val accentBarGradient = LinearGradient(
                cx, cy, cx, cy + cellH,
                accentColor, Color.parseColor("#00${Integer.toHexString(accentColor).substring(2)}"),
                Shader.TileMode.CLAMP
            )
            canvas.save()
            val accentClip = Path().apply {
                addRoundRect(RectF(cx, cy, cx + cellW, cy + cellH), 20f, 20f, Path.Direction.CW)
            }
            canvas.clipPath(accentClip)
            canvas.drawRect(cx, cy, cx + accentBarW, cy + cellH,
                Paint().apply { shader = accentBarGradient; isAntiAlias = true })
            canvas.restore()

            // Borde tarjeta
            val borderGradient = LinearGradient(
                cx, cy, cx + cellW, cy + cellH,
                colorCardBorder, Color.parseColor("#3A4060"), Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(
                RectF(cx, cy, cx + cellW, cy + cellH), 20f, 20f,
                Paint().apply {
                    shader = borderGradient; style = Paint.Style.STROKE
                    strokeWidth = 1.5f; isAntiAlias = true
                }
            )

            // Imagen
            val imgH = cellH * imgRatio
            val imgLoaded = item.imagenPath?.let { loadAndCropBitmap(it, cellW.toInt(), imgH.toInt()) }

            if (imgLoaded != null) {
                canvas.save()
                val clipPath = Path().apply {
                    addRoundRect(RectF(cx, cy, cx + cellW, cy + cellH), 20f, 20f, Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(imgLoaded, null, RectF(cx, cy, cx + cellW, cy + imgH), null)
                val imgOverlay = LinearGradient(
                    0f, cy, 0f, cy + imgH,
                    Color.TRANSPARENT, Color.parseColor("#80000000"), Shader.TileMode.CLAMP
                )
                canvas.drawRect(cx, cy, cx + cellW, cy + imgH,
                    Paint().apply { shader = imgOverlay })
                canvas.restore()
            } else {
                val colorPairs = listOf(
                    intArrayOf(Color.parseColor("#1A1F35"), Color.parseColor("#6C63FF")),
                    intArrayOf(Color.parseColor("#1A1F35"), Color.parseColor("#48CAE4")),
                    intArrayOf(Color.parseColor("#1A1F35"), Color.parseColor("#FF6B9D")),
                    intArrayOf(Color.parseColor("#1A1F35"), Color.parseColor("#FFD166"))
                )
                val colorPair = colorPairs[index % colorPairs.size]
                val placeholderGrad = LinearGradient(
                    cx, cy, cx + cellW, cy + imgH,
                    colorPair[0], colorPair[1], Shader.TileMode.CLAMP
                )
                canvas.save()
                val clipPath = Path().apply {
                    addRoundRect(RectF(cx, cy, cx + cellW, cy + cellH), 20f, 20f, Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                canvas.drawRect(cx, cy, cx + cellW, cy + imgH,
                    Paint().apply { shader = placeholderGrad })
                canvas.restore()

                val noImgPaint = Paint().apply { color = colorWhite30; textSize = 26f; isAntiAlias = true }
                val noImgText = "Sin imagen"
                canvas.drawText(noImgText,
                    cx + cellW / 2f - noImgPaint.measureText(noImgText) / 2f,
                    cy + imgH / 2f + 10f, noImgPaint)
            }

            // Badge estado
            val badgeTextPaint = Paint().apply {
                color = colorWhite; textSize = 20f; isFakeBoldText = true; isAntiAlias = true
            }
            val badgeText = item.estado
            val badgeW = badgeTextPaint.measureText(badgeText) + 22f
            val badgeX = cx + cellW - badgeW - 10f
            val badgeY = cy + imgH - 28f
            val badgeGradient = LinearGradient(
                badgeX, badgeY, badgeX + badgeW, badgeY + 24f,
                colorAccent1, colorAccent2, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + 24f),
                12f, 12f, Paint().apply { shader = badgeGradient; isAntiAlias = true })
            canvas.drawText(badgeText, badgeX + 11f, badgeY + 17f, badgeTextPaint)

            // Zona texto
            val textAreaTop = cy + imgH
            val textAreaH = cellH - imgH
            val lineH = textAreaH / 4f

            val titlePaint = Paint().apply {
                color = colorWhite; textSize = 27f; isFakeBoldText = true; isAntiAlias = true
            }
            var titulo = item.titulo
            while (titlePaint.measureText(titulo) > cellW - 28f && titulo.length > 3)
                titulo = titulo.dropLast(1)
            if (titulo != item.titulo) titulo += "…"
            canvas.drawText(titulo, cx + 14f, textAreaTop + lineH * 1f, titlePaint)

            val valorPaint = Paint().apply {
                color = colorGold; textSize = 25f; isFakeBoldText = true; isAntiAlias = true
            }
            canvas.drawText("${"%.2f".format(item.valor)} €", cx + 14f, textAreaTop + lineH * 2f, valorPaint)

            drawMiniStars(canvas, cx + 14f, textAreaTop + lineH * 3f, item.calificacion)
        }
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private fun drawFooter(canvas: Canvas, coleccion: Coleccion, items: List<Item>) {
        val footerTop = size * 0.895f
        val totalValor = items.sumOf { it.valor }
        val avgRating = if (items.isEmpty()) 0f else items.map { it.calificacion }.average().toFloat()

        val stats = listOf(
            Triple("${items.size}", "items", colorAccent2),
            Triple("${"%.0f".format(totalValor)} €", "valor total", colorGold)
        )

        val statW = (size - padding * 2) / 3f

        stats.forEachIndexed { i, (value, label, accentColor) ->
            val cx = padding + i * statW + statW / 2f

            val valuePaint = Paint().apply {
                this.color = accentColor; textSize = 38f; isFakeBoldText = true; isAntiAlias = true
            }
            canvas.drawText(value,
                cx - valuePaint.measureText(value) / 2f,
                footerTop + 42f, valuePaint)

            val labelPaint = Paint().apply {
                this.color = colorWhite60; textSize = 22f; isAntiAlias = true
            }
            canvas.drawText(label,
                cx - labelPaint.measureText(label) / 2f,
                footerTop + 78f, labelPaint)

            if (i < 1) {
                canvas.drawRect(
                    padding + (i + 1) * statW - 1f, footerTop + 10f,
                    padding + (i + 1) * statW + 1f, footerTop + 86f,
                    Paint().apply { color = colorWhite30 }
                )
            }
        }

        // Separador antes de valoración
        canvas.drawRect(
            padding + 2 * statW - 1f, footerTop + 10f,
            padding + 2 * statW + 1f, footerTop + 86f,
            Paint().apply { color = colorWhite30 }
        )

        // Valoración
        val ratingCx = padding + 2 * statW + statW / 2f

        val ratingValuePaint = Paint().apply {
            this.color = colorAccent3; textSize = 38f; isFakeBoldText = true; isAntiAlias = true
        }
        val ratingText = "${"%.1f".format(avgRating)}"
        canvas.drawText(ratingText,
            ratingCx - ratingValuePaint.measureText(ratingText) / 2f,
            footerTop + 42f, ratingValuePaint)

        val starsW = 5 * 16f + 4 * 5f
        val starsX = ratingCx - starsW / 2f
        drawMiniStars(canvas, starsX, footerTop + 76f, avgRating)

        val ratingLabelPaint = Paint().apply {
            this.color = colorWhite60; textSize = 22f; isAntiAlias = true
        }
        val labelText = "valoración"
        canvas.drawText(labelText,
            ratingCx - ratingLabelPaint.measureText(labelText) / 2f,
            footerTop + 96f, ratingLabelPaint)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun drawMiniStars(canvas: Canvas, x: Float, y: Float, rating: Float) {
        val starRadius = 10f
        val innerRadius = 4f
        val gap = 6f
        val points = 5

        for (i in 1..5) {
            val cx = x + (i - 1) * (starRadius * 2 + gap) + starRadius
            val cy = y - starRadius

            val path = Path()
            for (j in 0 until points * 2) {
                val angle = (Math.PI / points * j - Math.PI / 2).toFloat()
                val r = if (j % 2 == 0) starRadius else innerRadius
                val px = cx + r * Math.cos(angle.toDouble()).toFloat()
                val py = cy + r * Math.sin(angle.toDouble()).toFloat()
                if (j == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            val filled = i <= rating
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                if (filled) {
                    shader = LinearGradient(
                        cx - starRadius, cy - starRadius,
                        cx + starRadius, cy + starRadius,
                        colorGold, colorAccent3, Shader.TileMode.CLAMP
                    )
                } else {
                    color = colorWhite30
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun loadAndCropBitmap(path: String, targetW: Int, targetH: Int): Bitmap? {
        return try {
            val bmp = BitmapFactory.decodeFile(path) ?: return null
            val srcW = bmp.width.toFloat()
            val srcH = bmp.height.toFloat()
            val scale = maxOf(targetW / srcW, targetH / srcH)
            val scaledW = (srcW * scale).toInt()
            val scaledH = (srcH * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bmp, scaledW, scaledH, true)
            val offsetX = (scaledW - targetW) / 2
            val offsetY = (scaledH - targetH) / 2
            Bitmap.createBitmap(scaled, offsetX.coerceAtLeast(0), offsetY.coerceAtLeast(0), targetW, targetH)
        } catch (e: Exception) { null }
    }
}