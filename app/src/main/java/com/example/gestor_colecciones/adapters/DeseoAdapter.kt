package com.example.gestor_colecciones.adapters

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.ItemDeseo

class DeseoAdapter(
    private var items: List<ItemDeseo>,
    private val onConseguido: (ItemDeseo) -> Unit,
    private val onLongClick: (ItemDeseo) -> Unit
) : RecyclerView.Adapter<DeseoAdapter.DeseoViewHolder>() {

    inner class DeseoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPrioridad: TextView = itemView.findViewById(R.id.tvPrioridad)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val btnEnlace: MaterialButton = itemView.findViewById(R.id.btnEnlace)
        val btnConseguido: MaterialButton = itemView.findViewById(R.id.btnConseguido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeseoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deseo, parent, false)
        return DeseoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeseoViewHolder, position: Int) {
        val item = items[position]

        // Prioridad como emoji
        holder.tvPrioridad.text = when (item.prioridad) {
            1 -> "🔴"
            2 -> "🟡"
            else -> "🟢"
        }

        holder.tvTitulo.text = item.titulo

        // Tachar título si ya está conseguido
        if (item.conseguido) {
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.alpha = 0.5f
            holder.btnConseguido.visibility = View.GONE
        } else {
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.itemView.alpha = 1f
            holder.btnConseguido.visibility = View.VISIBLE
        }

        // Descripción
        if (!item.descripcion.isNullOrBlank()) {
            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = item.descripcion
        } else {
            holder.tvDescripcion.visibility = View.GONE
        }

        // Precio
        holder.tvPrecio.text = if (item.precioObjetivo > 0)
            "${"%.2f".format(item.precioObjetivo)}€"
        else
            "Sin precio"

        // Enlace
        if (!item.enlace.isNullOrBlank()) {
            holder.btnEnlace.visibility = View.VISIBLE
            holder.btnEnlace.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.enlace))
                it.context.startActivity(intent)
            }
        } else {
            holder.btnEnlace.visibility = View.GONE
        }

        holder.btnConseguido.setOnClickListener { onConseguido(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount() = items.size

    fun updateList(nuevos: List<ItemDeseo>) {
        items = nuevos
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ItemDeseo = items[position]
}