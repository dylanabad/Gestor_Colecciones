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

/**
 * Adaptador para mostrar una lista de deseos ([ItemDeseo]) en un [RecyclerView].
 *
 * Soporta la visualización de elementos pendientes y completados, permitiendo colapsar
 * o expandir la sección de elementos ya conseguidos mediante una cabecera dinámica.
 *
 * @param allItems Lista completa original de deseos.
 * @param onConseguido Callback invocado cuando un deseo se marca como conseguido.
 * @param onLongClick Callback invocado al realizar una pulsación larga sobre un deseo.
 */
class DeseoAdapter(
    private var allItems: List<ItemDeseo>,
    private val onConseguido: (ItemDeseo) -> Unit,
    private val onLongClick: (ItemDeseo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Lista procesada que incluye tanto los elementos [ItemDeseo] como las cabeceras [HeaderCompletados]. */
    private var displayItems: List<Any> = emptyList()

    /** Determina si la sección de elementos completados está expandida o colapsada. */
    private var isExpanded: Boolean = false

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_HEADER = 1
    }

    init {
        processItems()
    }

    /**
     * Procesa la lista [allItems] para generar [displayItems], organizando los elementos
     * en pendientes y completados, e insertando la cabecera si es necesario.
     */
    private fun processItems() {
        val pendientes = allItems.filter { !it.conseguido }
        val completados = allItems.filter { it.conseguido }
        
        val newList = mutableListOf<Any>()
        newList.addAll(pendientes)
        
        if (completados.isNotEmpty()) {
            newList.add(HeaderCompletados(completados.size))
            if (isExpanded) {
                newList.addAll(completados)
            }
        }
        displayItems = newList
    }

    /**
     * Clase de datos que representa la cabecera de la sección de elementos completados.
     * @property count Número total de deseos completados.
     */
    data class HeaderCompletados(val count: Int)

    /**
     * ViewHolder para los elementos de deseo individuales.
     */
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

    /**
     * ViewHolder para la cabecera de la sección de completados.
     */
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeaderTitle: TextView = itemView.findViewById(R.id.tvHeaderTitle)
        val ivExpandIcon: android.widget.ImageView = itemView.findViewById(R.id.ivExpandIcon)
        val headerContainer: View = itemView.findViewById(R.id.headerContainer)
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is HeaderCompletados -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_deseos_header, parent, false))
        } else {
            DeseoViewHolder(inflater.inflate(R.layout.item_deseo, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemData = displayItems[position]

        if (holder is HeaderViewHolder && itemData is HeaderCompletados) {
            holder.tvHeaderTitle.text = "Completados (${itemData.count})"
            
            // Rotación inicial sin animación para evitar saltos al hacer scroll
            holder.ivExpandIcon.rotation = if (isExpanded) 180f else 0f

            holder.headerContainer.setOnClickListener {
                val headerPosition = holder.adapterPosition
                if (headerPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                isExpanded = !isExpanded
                
                // Obtenemos los completados para animar solo el rango afectado
                val completados = allItems.filter { it.conseguido }
                
                processItems()

                if (isExpanded) {
                    notifyItemRangeInserted(headerPosition + 1, completados.size)
                } else {
                    notifyItemRangeRemoved(headerPosition + 1, completados.size)
                }
                
                // Animación suave del icono
                holder.ivExpandIcon.animate()
                    .rotation(if (isExpanded) 180f else 0f)
                    .setDuration(300)
                    .start()
            }
        }
 else if (holder is DeseoViewHolder && itemData is ItemDeseo) {
            bindDeseo(holder, itemData)
        }
    }

    /**
     * Vincula los datos de un [ItemDeseo] con su vista correspondiente, aplicando
     * estilos visuales según su prioridad y estado de "conseguido".
     */
    private fun bindDeseo(holder: DeseoViewHolder, item: ItemDeseo) {
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
            holder.tvTitulo.paintFlags = holder.tvTitulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            val colorSurface = com.google.android.material.color.MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant)
            holder.cardDeseo.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorSurface))
            holder.cardDeseo.strokeWidth = 0
            holder.tvTitulo.alpha = 0.5f
            holder.tvDescripcion.alpha = 0.5f
            holder.tvPrecio.alpha = 0.5f
            holder.cvPrioridad.alpha = 0.5f
            holder.btnConseguido.visibility = View.GONE
        } else {
            holder.tvTitulo.paintFlags = holder.tvTitulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            val colorSurface = com.google.android.material.color.MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurface)
            holder.cardDeseo.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(colorSurface))
            holder.cardDeseo.strokeWidth = (2 * holder.itemView.context.resources.displayMetrics.density).toInt()
            holder.tvTitulo.alpha = 1f
            holder.tvDescripcion.alpha = 1f
            holder.tvPrecio.alpha = 1f
            holder.cvPrioridad.alpha = 1f
            holder.btnConseguido.visibility = View.VISIBLE
        }

        // --- OTROS DETALLES ---
        holder.tvDescripcion.visibility = if (!item.descripcion.isNullOrBlank()) View.VISIBLE else View.GONE
        holder.tvDescripcion.text = item.descripcion ?: ""
        
        holder.tvPrecio.text = if (item.precioObjetivo > 0) "${"%.2f".format(item.precioObjetivo)} €" else "Sin precio"

        if (!item.enlace.isNullOrBlank()) {
            holder.btnEnlace.visibility = View.VISIBLE
            holder.btnEnlace.setOnClickListener {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.enlace)))
            }
        } else {
            holder.btnEnlace.visibility = View.GONE
        }

        holder.btnConseguido.setOnClickListener { onConseguido(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount() = displayItems.size

    /**
     * Actualiza la lista de deseos utilizando [DiffUtil] para calcular los cambios de forma eficiente.
     * @param nuevos Nueva lista de deseos a mostrar.
     */
    fun updateList(nuevos: List<ItemDeseo>) {
        val oldDisplayItems = displayItems
        allItems = nuevos
        
        // Calculamos la nueva lista de visualización
        val pendientes = allItems.filter { !it.conseguido }
        val completados = allItems.filter { it.conseguido }
        val newList = mutableListOf<Any>()
        newList.addAll(pendientes)
        if (completados.isNotEmpty()) {
            newList.add(HeaderCompletados(completados.size))
            if (isExpanded) newList.addAll(completados)
        }
        
        val newDisplayItems = newList

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldDisplayItems.size
            override fun getNewListSize() = newDisplayItems.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = oldDisplayItems[oldPos]
                val new = newDisplayItems[newPos]
                return if (old is ItemDeseo && new is ItemDeseo) {
                    old.id == new.id
                } else if (old is HeaderCompletados && new is HeaderCompletados) {
                    true
                } else false
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return oldDisplayItems[oldPos] == newDisplayItems[newPos]
            }
        })

        displayItems = newDisplayItems
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Obtiene el [ItemDeseo] en la posición especificada.
     * @throws IllegalStateException Si el elemento en esa posición no es de tipo [ItemDeseo].
     */
    fun getItem(position: Int): ItemDeseo {
        val data = displayItems[position]
        return if (data is ItemDeseo) data else throw IllegalStateException("Not an item")
    }
}
