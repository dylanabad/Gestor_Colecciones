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
import java.util.Locale
import com.github.mikephil.charting.animation.Easing
import android.R.attr.colorPrimary

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
                        renderStats(state.data)
                    }
                    is StatsState.Error -> {
                        // En caso de error ocultar el chart (podría añadirse manejo de error más explícito)
                        binding.cardChart.visibility = View.GONE
                    }
                }
            }
        }
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
