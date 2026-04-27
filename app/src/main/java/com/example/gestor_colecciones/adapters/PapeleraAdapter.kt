package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.example.gestor_colecciones.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Representa un elemento que ha sido enviado a la papelera.
 *
 * @property id Identificador del elemento.
 * @property icono Icono representativo (emoji o símbolo).
 * @property nombre Nombre del elemento eliminado.
 * @property meta Información adicional (ej: nombre de la colección original).
 * @property fechaEliminacion Instante en el que se realizó el borrado.
 * @property diasRestantes Tiempo de vida restante antes de la purga automática.
 */
data class PapeleraItem(
    val id: Int,
    val icono: String,
    val nombre: String,
    val meta: String,
    val fechaEliminacion: Date,
    val diasRestantes: Int
)

/**
 * Adaptador para visualizar y gestionar los elementos eliminados temporalmente.
 *
 * Permite la restauración de elementos o su gestión mediante pulsación larga.
 * Muestra advertencias visuales si la eliminación definitiva es inminente.
 *
 * @property items Lista de [PapeleraItem] actualmente en la papelera.
 * @property onRestaurar Callback para solicitar la recuperación de un elemento.
 * @property onLongClick Callback para abrir opciones adicionales sobre un elemento.
 */
class PapeleraAdapter(
    private var items: List<PapeleraItem>,
    private val onRestaurar: (PapeleraItem) -> Unit,
    private val onLongClick: (PapeleraItem) -> Unit
) : RecyclerView.Adapter<PapeleraAdapter.ViewHolder>() {

    // Formateador de fecha para mostrar la fecha de eliminación
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ViewHolder que contiene las vistas de cada item de la papelera
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        val tvFechaEliminacion: TextView = itemView.findViewById(R.id.tvFechaEliminacion)
        val btnRestaurar: MaterialButton = itemView.findViewById(R.id.btnRestaurar)
    }

    // Inflado del layout de cada item de papelera
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_papelera, parent, false)

        return ViewHolder(view)
    }

    // Vinculación de datos con la vista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        holder.tvIcono.text = item.icono
        holder.tvNombre.text = item.nombre
        holder.tvMeta.text = item.meta

        // --- TEXTO DE ESTADO SEGÚN DÍAS RESTANTES ---
        holder.tvFechaEliminacion.text = when {

            // Eliminación inminente
            item.diasRestantes <= 1 ->
                "⚠️ Se elimina hoy"

            // Aviso de pocos días restantes
            item.diasRestantes <= 3 ->
                "⚠️ ${item.diasRestantes} días restantes"

            // Estado normal con fecha completa
            else ->
                "Eliminado el ${dateFormat.format(item.fechaEliminacion)} · ${item.diasRestantes} días restantes"
        }

        // Acción de restaurar elemento
        holder.btnRestaurar.setOnClickListener {
            onRestaurar(item)
        }

        // Acción de click largo sobre el item
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    // Número total de elementos en la papelera
    override fun getItemCount() = items.size

    /**
     * Actualiza la lista de elementos en la papelera y refresca la vista.
     */
    fun updateList(nuevos: List<PapeleraItem>) {
        items = nuevos
        notifyDataSetChanged()
    }

    // Devuelve un elemento por posición
    fun getItem(position: Int) = items[position]
}