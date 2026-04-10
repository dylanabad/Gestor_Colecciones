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

// Data class que representa un elemento dentro de la papelera
data class PapeleraItem(
    val id: Int,                    // ID del elemento eliminado
    val icono: String,             // Icono representativo
    val nombre: String,            // Nombre del elemento
    val meta: String,              // Información adicional o descripción corta
    val fechaEliminacion: Date,    // Fecha en la que fue eliminado
    val diasRestantes: Int         // Días restantes antes de eliminación definitiva
)

// Adapter para mostrar elementos de la papelera en un RecyclerView
class PapeleraAdapter(
    private var items: List<PapeleraItem>,               // Lista de elementos en papelera
    private val onRestaurar: (PapeleraItem) -> Unit,    // Callback para restaurar elemento
    private val onLongClick: (PapeleraItem) -> Unit     // Callback para acciones de click largo
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

    // Actualiza la lista de elementos
    fun updateList(nuevos: List<PapeleraItem>) {
        items = nuevos
        notifyDataSetChanged()
    }

    // Devuelve un elemento por posición
    fun getItem(position: Int) = items[position]
}