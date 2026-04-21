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
import androidx.recyclerview.widget.DiffUtil
import com.example.gestor_colecciones.entities.ItemDeseo

// Adapter para mostrar lista de deseos en un RecyclerView
class DeseoAdapter(
    private var items: List<ItemDeseo>,                 // Lista de elementos de deseo
    private val onConseguido: (ItemDeseo) -> Unit,     // Callback cuando se marca como conseguido
    private val onLongClick: (ItemDeseo) -> Unit        // Callback para click largo (acciones extra)
) : RecyclerView.Adapter<DeseoAdapter.DeseoViewHolder>() {

    // ViewHolder que contiene las vistas de cada item de la lista
    inner class DeseoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvPrioridad: TextView = itemView.findViewById(R.id.tvPrioridad)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val btnEnlace: MaterialButton = itemView.findViewById(R.id.btnEnlace)
        val btnConseguido: MaterialButton = itemView.findViewById(R.id.btnConseguido)
    }

    // Inflado del layout de cada item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeseoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deseo, parent, false)
        return DeseoViewHolder(view)
    }

    // Vinculación de datos con la vista
    override fun onBindViewHolder(holder: DeseoViewHolder, position: Int) {

        val item = items[position]

        // --- PRIORIDAD VISUAL ---
        holder.tvPrioridad.text = when (item.prioridad) {
            1 -> "🔴"   // alta prioridad
            2 -> "🟡"   // media prioridad
            else -> "🟢" // baja prioridad
        }

        holder.tvTitulo.text = item.titulo

        // --- ESTADO CONSEGUIDO ---
        if (item.conseguido) {

            // Texto tachado si el item ya se consiguió
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Reduce opacidad del item completo
            holder.itemView.alpha = 0.5f

            // Oculta botón de "conseguido"
            holder.btnConseguido.visibility = View.GONE

        } else {

            // Quita tachado si no está conseguido
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            holder.itemView.alpha = 1f

            holder.btnConseguido.visibility = View.VISIBLE
        }

        // --- DESCRIPCIÓN ---
        if (!item.descripcion.isNullOrBlank()) {

            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = item.descripcion

        } else {

            holder.tvDescripcion.visibility = View.GONE
        }

        // --- PRECIO OBJETIVO ---
        holder.tvPrecio.text =
            if (item.precioObjetivo > 0)
                "${"%.2f".format(item.precioObjetivo)} €"
            else
                "Sin precio"

        // --- ENLACE EXTERNO ---
        if (!item.enlace.isNullOrBlank()) {

            holder.btnEnlace.visibility = View.VISIBLE

            holder.btnEnlace.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.enlace))
                it.context.startActivity(intent)
            }

        } else {

            holder.btnEnlace.visibility = View.GONE
        }

        // --- ACCIONES ---
        holder.btnConseguido.setOnClickListener { onConseguido(item) }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    // Número de elementos en la lista
    override fun getItemCount() = items.size

    // Clase interna para calcular diferencias entre listas para animaciones fluidas
    class DeseoDiffCallback(
        private val oldList: List<ItemDeseo>,
        private val newList: List<ItemDeseo>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }

    // Actualiza la lista usando DiffUtil para animar solo los cambios necesarios
    fun updateList(nuevos: List<ItemDeseo>) {
        val diffCallback = DeseoDiffCallback(items, nuevos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = nuevos
        diffResult.dispatchUpdatesTo(this)
    }

    // Obtiene un item por posición
    fun getItem(position: Int): ItemDeseo = items[position]
}