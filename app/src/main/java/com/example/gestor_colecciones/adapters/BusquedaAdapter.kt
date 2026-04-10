package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.example.gestor_colecciones.R

// Clase sellada que representa los distintos tipos de elementos en la búsqueda
sealed class BusquedaItem {

    // Elemento tipo encabezado (sección)
    data class Header(val titulo: String) : BusquedaItem()

    // Elemento tipo resultado (ítem clicable de búsqueda)
    data class Resultado(
        val id: Int,               // ID del elemento
        val icono: String,        // Icono representado como texto (emoji o símbolo)
        val titulo: String,       // Título principal del resultado
        val subtitulo: String,    // Descripción secundaria
        val tipo: String,         // Tipo de elemento (ej: colección, item, etc.)
        val esColeccion: Boolean  // Indica si pertenece a una colección
    ) : BusquedaItem()
}

// Adapter para RecyclerView que maneja headers y resultados
class BusquedaAdapter(
    private var items: List<BusquedaItem>,                       // Lista de elementos a mostrar
    private val onClick: (BusquedaItem.Resultado) -> Unit        // Callback al pulsar un resultado
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0      // Tipo de vista header
        private const val TYPE_RESULTADO = 1   // Tipo de vista resultado
    }

    // ViewHolder para los encabezados
    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvBusquedaHeader)
    }

    // ViewHolder para los resultados
    inner class ResultadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcono: TextView = view.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvSubtitulo: TextView = view.findViewById(R.id.tvSubtitulo)
        val chipTipo: Chip = view.findViewById(R.id.chipTipo)
    }

    // Devuelve el tipo de vista según el tipo de item
    override fun getItemViewType(position: Int) =
        if (items[position] is BusquedaItem.Header) TYPE_HEADER else TYPE_RESULTADO

    // Crea el ViewHolder adecuado según el tipo de vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {

            // Inflado del layout de header
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_busqueda_header, parent, false)
            HeaderViewHolder(view)

        } else {

            // Inflado del layout de resultado
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_busqueda_resultado, parent, false)
            ResultadoViewHolder(view)
        }
    }

    // Vincula los datos con la vista correspondiente
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            // Si es header, solo se asigna el título
            is BusquedaItem.Header ->
                (holder as HeaderViewHolder).tvHeader.text = item.titulo

            // Si es resultado, se rellenan todos los campos
            is BusquedaItem.Resultado -> {
                (holder as ResultadoViewHolder).apply {

                    tvIcono.text = item.icono
                    tvTitulo.text = item.titulo
                    tvSubtitulo.text = item.subtitulo
                    chipTipo.text = item.tipo

                    // Click en el item -> callback
                    itemView.setOnClickListener { onClick(item) }
                }
            }
        }
    }

    // Número total de elementos
    override fun getItemCount() = items.size

    // Actualiza la lista de datos del adapter
    fun updateList(nuevos: List<BusquedaItem>) {
        items = nuevos
        notifyDataSetChanged()
    }
}