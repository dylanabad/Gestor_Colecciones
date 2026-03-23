package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.example.gestor_colecciones.R

sealed class BusquedaItem {
    data class Header(val titulo: String) : BusquedaItem()
    data class Resultado(
        val id: Int,
        val icono: String,
        val titulo: String,
        val subtitulo: String,
        val tipo: String,
        val esColeccion: Boolean
    ) : BusquedaItem()
}

class BusquedaAdapter(
    private var items: List<BusquedaItem>,
    private val onClick: (BusquedaItem.Resultado) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_RESULTADO = 1
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvBusquedaHeader)
    }

    inner class ResultadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcono: TextView = view.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvSubtitulo: TextView = view.findViewById(R.id.tvSubtitulo)
        val chipTipo: Chip = view.findViewById(R.id.chipTipo)
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is BusquedaItem.Header) TYPE_HEADER else TYPE_RESULTADO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_busqueda_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_busqueda_resultado, parent, false)
            ResultadoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is BusquedaItem.Header -> (holder as HeaderViewHolder).tvHeader.text = item.titulo
            is BusquedaItem.Resultado -> {
                (holder as ResultadoViewHolder).apply {
                    tvIcono.text = item.icono
                    tvTitulo.text = item.titulo
                    tvSubtitulo.text = item.subtitulo
                    chipTipo.text = item.tipo
                    itemView.setOnClickListener { onClick(item) }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(nuevos: List<BusquedaItem>) {
        items = nuevos
        notifyDataSetChanged()
    }
}