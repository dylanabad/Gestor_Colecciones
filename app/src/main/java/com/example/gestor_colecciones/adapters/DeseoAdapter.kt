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

        val cardDeseo: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardDeseo)
        val cvPrioridad: View = itemView.findViewById(R.id.cvPrioridad)
        val ivPrioridad: android.widget.ImageView = itemView.findViewById(R.id.ivPrioridad)
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
        when (item.prioridad) {
            1 -> {
                holder.tvPrioridad.text = "ALTA"
                holder.ivPrioridad.setImageResource(R.drawable.ic_priority_high)
            }
            2 -> {
                holder.tvPrioridad.text = "MEDIA"
                holder.ivPrioridad.setImageResource(R.drawable.ic_priority_medium)
            }
            else -> {
                holder.tvPrioridad.text = "BAJA"
                holder.ivPrioridad.setImageResource(R.drawable.ic_priority_low)
            }
        }

        holder.tvTitulo.text = item.titulo

        // --- ESTADO CONSEGUIDO ---
        if (item.conseguido) {

            // Texto tachado si el item ya se consiguió
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Efecto de oscurecimiento: cambiamos el fondo de la tarjeta
            // En modo claro será un gris tenue, en modo noche un tono aún más oscuro
            val colorSurface = com.google.android.material.color.MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant)
            holder.cardDeseo.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorSurface))
            holder.cardDeseo.strokeWidth = 0 // Quitamos el borde para que parezca más "hundido"

            // Bajamos la opacidad de los textos para que se sientan inactivos
            holder.tvTitulo.alpha = 0.5f
            holder.tvDescripcion.alpha = 0.5f
            holder.tvPrecio.alpha = 0.5f
            holder.cvPrioridad.alpha = 0.5f

            // Oculta botón de "conseguido"
            holder.btnConseguido.visibility = View.GONE

        } else {

            // Quita tachado si no está conseguido
            holder.tvTitulo.paintFlags =
                holder.tvTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            // Restauramos fondo y borde original
            val colorSurface = com.google.android.material.color.MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurface)
            holder.cardDeseo.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorSurface))
            holder.cardDeseo.strokeWidth = (2 * holder.itemView.context.resources.displayMetrics.density).toInt()

            // Restauramos opacidad
            holder.tvTitulo.alpha = 1f
            holder.tvDescripcion.alpha = 1f
            holder.tvPrecio.alpha = 1f
            holder.cvPrioridad.alpha = 1f

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