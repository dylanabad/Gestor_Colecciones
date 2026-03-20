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

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val file: File, val share: Boolean = false) : ExportState()
    data class Error(val message: String) : ExportState()
}

class ExportViewModel(
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun exportCsv(context: Context, share: Boolean = false, ids: List<Int>? = null) =
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            runCatching {
                val data = exportRepository.getDataForExport(ids)
                CsvExporter(context).export(data)
            }.onSuccess {
                _exportState.value = ExportState.Success(it, share)
            }.onFailure {
                _exportState.value = ExportState.Error(it.message ?: "Error al exportar CSV")
            }
        }

    fun exportPdf(context: Context, share: Boolean = false, ids: List<Int>? = null) =
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            runCatching {
                val data = exportRepository.getDataForExport(ids)
                PdfExporter(context).export(data)
            }.onSuccess {
                _exportState.value = ExportState.Success(it, share)
            }.onFailure {
                _exportState.value = ExportState.Error(it.message ?: "Error al exportar PDF")
            }
        }

    fun exportCatalogoPdf(context: Context, share: Boolean = false, ids: List<Int>? = null) =
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            runCatching {
                val data = exportRepository.getDataForExport(ids)
                CatalogoPdfExporter(context).export(data)
            }.onSuccess {
                _exportState.value = ExportState.Success(it, share)
            }.onFailure {
                _exportState.value = ExportState.Error(it.message ?: "Error al generar catálogo")
            }
        }

    fun resetState() { _exportState.value = ExportState.Idle }
}