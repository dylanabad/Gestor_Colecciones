package com.example.gestor_colecciones.fragment

/*
 * StatsFragment.kt
 *
 * Fragmento que muestra estadísticas y un gráfico con el valor total por colección.
 * Consulta datos de exportación (colecciones e items) mediante un ViewModel y
 * renderiza un resumen numérico y un BarChart usando MPAndroidChart.
 *
 */

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentStatsBinding
import com.example.gestor_colecciones.repository.ColeccionExportData
import com.example.gestor_colecciones.repository.ExportRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.viewmodel.StatsState
import com.example.gestor_colecciones.viewmodel.StatsViewModel
import com.example.gestor_colecciones.viewmodel.StatsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.pdf.PdfDocument
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.R.attr.colorPrimary
import android.graphics.Paint
import android.graphics.Rect
import java.util.Locale
import com.github.mikephil.charting.animation.Easing

/**
 * Resume indicadores agregados del inventario del usuario para la vista de estadisticas.
 */
class StatsFragment : Fragment() {

    // ViewBinding para acceder a las vistas del layout
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    // ViewModel que expone el estado de la carga y los datos de estadística
    private lateinit var viewModel: StatsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurar transiciones visuales suaves para la entrada/salida del fragmento
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar layout y preparar ViewBinding
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preparar repositorios y ViewModel que obtendrá los datos para las estadísticas
        val coleccionRepo = RepositoryProvider.coleccionRepository(requireContext())
        val itemRepo = RepositoryProvider.itemRepository(requireContext())
        val exportRepo = ExportRepository(coleccionRepo, itemRepo)

        viewModel = ViewModelProvider(
            this, StatsViewModelFactory(exportRepo)
        )[StatsViewModel::class.java]

        // Botón para volver atrás
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Observar el estado del ViewModel y actualizar la UI según el estado (loading/success/error)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is StatsState.Loading -> {
                        binding.cardChart.visibility = View.GONE
                        binding.cardMainStats.visibility = View.GONE
                        binding.layoutSubStats.visibility = View.GONE
                        binding.tvChartTitle.visibility = View.GONE
                    }
                    is StatsState.Success -> {
                        binding.cardChart.visibility = View.VISIBLE
                        binding.cardMainStats.visibility = View.VISIBLE
                        binding.layoutSubStats.visibility = View.VISIBLE
                        binding.tvChartTitle.visibility = View.VISIBLE
                        binding.btnExport.visibility = View.VISIBLE
                        renderStats(state.data)

                        binding.btnExport.setOnClickListener {
                            exportStats(state.data)
                        }
                    }
                    is StatsState.Error -> {
                        // En caso de error ocultar el chart (podría añadirse manejo de error más explícito)
                        binding.cardChart.visibility = View.GONE
                        binding.btnExport.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun exportStats(data: List<ColeccionExportData>) {
        val totalItems = data.sumOf { it.items.size }
        val valorTotal = data.sumOf { entry -> entry.items.sumOf { it.valor } }

        val report = StringBuilder().apply {
            append("📊 REPORTE DE ESTADÍSTICAS - GESTOR DE COLECCIONES\n")
            append("==============================================\n\n")
            append("RESUMEN GLOBAL:\n")
            append("• Valor Total Estimado: ${String.format(Locale.getDefault(), "%.2f€", valorTotal)}\n")
            append("• Total de Colecciones: ${data.size}\n")
            append("• Total de Ítems: $totalItems\n\n")
            append("DESGLOSE POR COLECCIÓN:\n")
            append("----------------------------------------------\n")
            
            data.forEach { entry ->
                val colValor = entry.items.sumOf { it.valor }
                append("📂 ${entry.coleccion.nombre}\n")
                append("   - Ítems: ${entry.items.size}\n")
                append("   - Valor: ${String.format(Locale.getDefault(), "%.2f€", colValor)}\n")
                append("----------------------------------------------\n")
            }
            
            append("\nGenerado el: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}")
        }.toString()

        // Crear menú de opciones de exportación
        val options = arrayOf("Compartir Informe (PDF)", "Guardar en Dispositivo (PDF)", "Compartir como Imagen", "Copiar Texto")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Opciones de Exportación")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareAsPdf()
                    1 -> savePdfToDevice()
                    2 -> shareAsImage()
                    3 -> copyToClipboard(report)
                }
            }
            .show()
    }

    private fun captureView(): Bitmap {
        // Capturar únicamente la tarjeta que contiene el gráfico (cardChart)
        val view = binding.cardChart
        
        // Creamos el bitmap con las dimensiones exactas de la tarjeta
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Forzamos un fondo blanco para que el PDF/Imagen sea profesional y legible 
        // (evita transparencias o fondos oscuros del tema que dificulten la lectura)
        canvas.drawColor(Color.WHITE)
        
        // Dibujamos solo la vista del gráfico
        view.draw(canvas)

        return bitmap
    }

    private fun shareAsPdf() {
        val bitmap = captureView()
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val file = File(requireContext().cacheDir, "Estadisticas_Coleccion.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Compartir PDF"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePdfToDevice() {
        val bitmap = captureView()
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val fileName = "Estadisticas_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                Toast.makeText(requireContext(), "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
            }
        }
        pdfDocument.close()
    }

    private fun shareAsImage() {
        val bitmap = captureView()
        val file = File(requireContext().cacheDir, "Estadisticas.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir Imagen"))
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Estadísticas", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun renderStats(data: List<ColeccionExportData>) {
        // Renderizar resumen numérico
        val totalItems = data.sumOf { it.items.size }
        val valorTotal = data.sumOf { entry -> entry.items.sumOf { it.valor } }

        binding.tvTotalColecciones.text = data.size.toString()
        binding.tvTotalItems.text = totalItems.toString()
        binding.tvValorTotal.text = String.format(Locale.getDefault(), "%.2f€", valorTotal)

        // Preparar datos para el gráfico de barras
        val entries = data.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.items.sumOf { it.valor }.toFloat())
        }
        val labels = data.map { it.coleccion.nombre }

        val dataSet = BarDataSet(entries, "Valor").apply {
            // Usar el color primario del tema actual de forma segura
            color = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                android.R.attr.colorPrimary,
                Color.BLUE
            )
            
            valueTextColor = Color.GRAY
            valueTextSize = 11f
            setDrawValues(true)
            // Formatear valores de las barras con el símbolo €
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format(Locale.getDefault(), "%.1f€", value)
                }
            }
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        binding.barChart.apply {
            this.data = barData
            setExtraOffsets(0f, 0f, 0f, 15f) // Espacio para etiquetas rotadas

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textSize = 11f
                textColor = Color.GRAY
                setLabelCount(labels.size, false)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
                axisMinimum = 0f
                textSize = 11f
                textColor = Color.GRAY
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setFitBars(true)
            setScaleEnabled(false)
            setTouchEnabled(true)
            animateY(1000, Easing.EaseOutCubic)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar binding para evitar fugas de memoria
        _binding = null
    }
}
