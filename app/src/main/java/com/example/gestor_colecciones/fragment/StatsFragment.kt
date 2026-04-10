package com.example.gestor_colecciones.fragment

/*
 * StatsFragment.kt
 *
 * Fragmento que muestra estadísticas y un gráfico con el valor total por colección.
 * Consulta datos de exportación (colecciones e items) mediante un ViewModel y
 * renderiza un resumen numérico y un BarChart usando MPAndroidChart.
 *
 * Nota: Solo se añaden comentarios explicativos en español; no se modifica la lógica.
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
                        binding.cardResumen.visibility = View.GONE
                    }
                    is StatsState.Success -> {
                        binding.cardChart.visibility = View.VISIBLE
                        binding.cardResumen.visibility = View.VISIBLE
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
        binding.tvValorTotal.text = "${"%.2f".format(valorTotal)}€"

        // Preparar datos para el gráfico de barras: una entrada por colección con su valor total
        val entries = data.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.items.sumOf { it.valor }.toFloat())
        }
        val labels = data.map { it.coleccion.nombre }

        // Dataset del gráfico con estilo y color
        val dataSet = BarDataSet(entries, "Valor (€)").apply {
            val primaryColor = requireContext().getColor(
                com.google.android.material.R.color.material_dynamic_primary40
            )
            color = primaryColor
            valueTextColor = Color.DKGRAY
            valueTextSize = 10f
            setDrawValues(true)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        // Configurar el BarChart: ejes, comportamiento y animación
        binding.barChart.apply {
            this.data = barData

            // Eje X — nombres de colecciones
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -30f
                textSize = 10f
                setLabelCount(labels.size, false)
            }

            // Eje Y izquierdo — mostrar valores y líneas de grid
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textSize = 10f
            }

            // Desactivar elementos innecesarios y ajustar comportamiento
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setFitBars(true)
            setTouchEnabled(true)
            setPinchZoom(false)
            animateY(600)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar binding para evitar fugas de memoria
        _binding = null
    }
}
