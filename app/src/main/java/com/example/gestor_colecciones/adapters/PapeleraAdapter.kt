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

data class PapeleraItem(
    val id: Int,
    val icono: String,
    val nombre: String,
    val meta: String,
    val fechaEliminacion: Date,
    val diasRestantes: Int
)

class PapeleraAdapter(
    private var items: List<PapeleraItem>,
    private val onRestaurar: (PapeleraItem) -> Unit,
    private val onLongClick: (PapeleraItem) -> Unit
) : RecyclerView.Adapter<PapeleraAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        val tvFechaEliminacion: TextView = itemView.findViewById(R.id.tvFechaEliminacion)
        val btnRestaurar: MaterialButton = itemView.findViewById(R.id.btnRestaurar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_papelera, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcono.text = item.icono
        holder.tvNombre.text = item.nombre
        holder.tvMeta.text = item.meta
        holder.tvFechaEliminacion.text = when {
            item.diasRestantes <= 1 -> "⚠️ Se elimina hoy"
            item.diasRestantes <= 3 -> "⚠️ ${item.diasRestantes} días restantes"
            else -> "Eliminado el ${dateFormat.format(item.fechaEliminacion)} · ${item.diasRestantes} días restantes"
        }
        holder.btnRestaurar.setOnClickListener { onRestaurar(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount() = items.size

    fun updateList(nuevos: List<PapeleraItem>) {
        items = nuevos
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = items[position]
}