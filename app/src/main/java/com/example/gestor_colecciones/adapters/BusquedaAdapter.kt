package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R

/**
 * Representa los diferentes tipos de elementos que pueden aparecer en la lista de búsqueda.
 */
sealed class BusquedaItem {

    /**
     * Elemento de cabecera para separar secciones (ej: "Colecciones", "Ítems").
     * @property titulo Texto a mostrar en la cabecera.
     */
    data class Header(val titulo: String) : BusquedaItem()

    /**
     * Resultado individual de búsqueda clicable.
     * @property id Identificador único del elemento.
     * @property icono Representación visual (emoji o símbolo).
     * @property titulo Texto principal.
     * @property subtitulo Información secundaria o descripción.
     * @property tipo Categoría del resultado.
     * @property esColeccion Verdadero si el resultado es una colección completa.
     */
    data class Resultado(
        val id: Int,
        val icono: String,
        val titulo: String,
        val subtitulo: String,
        val tipo: String,
        val esColeccion: Boolean
    ) : BusquedaItem()
}

/**
 * Adaptador para [RecyclerView] que gestiona una lista mixta de cabeceras y resultados de búsqueda.
 *
 * @property items Lista de elementos ([BusquedaItem]) a visualizar.
 * @property onClick Callback que se ejecuta al seleccionar un resultado.
 */
class BusquedaAdapter(
    private var items: List<BusquedaItem>,
    private val onClick: (BusquedaItem.Resultado) -> Unit
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