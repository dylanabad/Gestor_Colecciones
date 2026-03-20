package com.example.gestor_colecciones.fragment

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
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ExportRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.StatsState
import com.example.gestor_colecciones.viewmodel.StatsViewModel
import com.example.gestor_colecciones.viewmodel.StatsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StatsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val coleccionRepo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        val itemRepo = ItemRepository(DatabaseProvider.getItemDao(requireContext()))
        val exportRepo = ExportRepository(coleccionRepo, itemRepo)

        viewModel = ViewModelProvider(
            this, StatsViewModelFactory(exportRepo)
        )[StatsViewModel::class.java]

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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
                        binding.cardChart.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun renderStats(data: List<ColeccionExportData>) {
        // Resumen
        val totalItems = data.sumOf { it.items.size }
        val valorTotal = data.sumOf { entry -> entry.items.sumOf { it.valor } }

        binding.tvTotalColecciones.text = data.size.toString()
        binding.tvTotalItems.text = totalItems.toString()
        binding.tvValorTotal.text = "${"%.2f".format(valorTotal)}€"

        // Gráfico de barras
        val entries = data.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.items.sumOf { it.valor }.toFloat())
        }
        val labels = data.map { it.coleccion.nombre }

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

            // Eje Y izquierdo
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textSize = 10f
            }

            // Desactivar elementos innecesarios
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
        _binding = null
    }
}