package com.example.gestor_colecciones.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.export.PdfExporter
import com.example.gestor_colecciones.export.CsvExporter
import com.example.gestor_colecciones.repository.ExportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.example.gestor_colecciones.export.CatalogoPdfExporter

// Estados posibles del proceso de exportación
sealed class ExportState {

    object Idle : ExportState()          // Sin exportación activa
    object Loading : ExportState()       // Exportación en curso

    data class Success(
        val file: File,
        val share: Boolean = false
    ) : ExportState()                    // Exportación completada correctamente

    data class Error(val message: String) : ExportState() // Error durante exportación
}

// ViewModel encargado de exportar datos a CSV o PDF
class ExportViewModel(
    private val exportRepository: ExportRepository
) : ViewModel() {

    // Estado interno de exportación
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)

    // Estado expuesto a la UI
    val exportState: StateFlow<ExportState> = _exportState

    // Exporta datos en formato CSV
    fun exportCsv(
        context: Context,
        share: Boolean = false,
        ids: List<Int>? = null
    ) = viewModelScope.launch {

        _exportState.value = ExportState.Loading

        runCatching {

            val data = exportRepository.getDataForExport(ids)
            CsvExporter(context).export(data)

        }.onSuccess {

            _exportState.value = ExportState.Success(it, share)

        }.onFailure {

            _exportState.value =
                ExportState.Error(it.message ?: "Error al exportar CSV")
        }
    }

    // Exporta datos en formato PDF estándar
    fun exportPdf(
        context: Context,
        share: Boolean = false,
        ids: List<Int>? = null
    ) = viewModelScope.launch {

        _exportState.value = ExportState.Loading

        runCatching {

            val data = exportRepository.getDataForExport(ids)
            PdfExporter(context).export(data)

        }.onSuccess {

            _exportState.value = ExportState.Success(it, share)

        }.onFailure {

            _exportState.value =
                ExportState.Error(it.message ?: "Error al exportar PDF")
        }
    }

    // Exporta un PDF tipo catálogo (formato más visual)
    fun exportCatalogoPdf(
        context: Context,
        share: Boolean = false,
        ids: List<Int>? = null
    ) = viewModelScope.launch {

        _exportState.value = ExportState.Loading

        runCatching {

            val data = exportRepository.getDataForExport(ids)
            CatalogoPdfExporter(context).export(data)

        }.onSuccess {

            _exportState.value = ExportState.Success(it, share)

        }.onFailure {

            _exportState.value =
                ExportState.Error(it.message ?: "Error al generar catálogo")
        }
    }

    // Reinicia el estado de exportación a Idle
    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}