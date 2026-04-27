package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Coleccion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import android.content.res.ColorStateList
import android.util.TypedValue
import com.example.gestor_colecciones.util.ImageUtils

/**
 * Adaptador para mostrar una lista de [Coleccion] en un [RecyclerView].
 *
 * Cada elemento visualiza el nombre, descripción, imagen (vía Glide) y un indicador
 * de color personalizado. También gestiona la visualización de estadísticas rápidas
 * asociadas a cada colección.
 *
 * @property colecciones Lista de colecciones a mostrar.
 * @property onClick Callback para gestionar el clic simple.
 * @property onLongClick Callback para gestionar el clic prolongado.
 * @property coleccionStats Mapa que vincula el ID de la colección con un texto de estadísticas (ej: "12 ítems").
 */
class ColeccionAdapter(
    private var colecciones: List<Coleccion>,
    private val onClick: (Coleccion) -> Unit,
    private val onLongClick: (Coleccion) -> Unit,
    private val coleccionStats: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ColeccionAdapter.ColeccionViewHolder>() {

    init {
        // Permite optimizar RecyclerView usando IDs estables
        setHasStableIds(true)
    }

    /**
     * ViewHolder que gestiona las referencias a las vistas del item de colección.
     */
    inner class ColeccionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val card: MaterialCardView = itemView.findViewById(R.id.cardColeccion)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreColeccion)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionColeccion)
        val tvStats: TextView = itemView.findViewById(R.id.tvStatsColeccion)
        val ivColeccion: ImageView = itemView.findViewById(R.id.ivColeccion)
        val colorDot: View = itemView.findViewById(R.id.viewColeccionColor)

        init {

            // Click normal en item
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION)
                    onClick(colecciones[position])
            }

            // Click largo en item
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION)
                    onLongClick(colecciones[position])
                true
            }
        }
    }

    // Inflado del layout del item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColeccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coleccion, parent, false)
        return ColeccionViewHolder(view)
    }

    // Vinculación de datos con la vista
    override fun onBindViewHolder(holder: ColeccionViewHolder, position: Int) {

        val coleccion = colecciones[position]

        // Datos básicos
        holder.tvNombre.text = coleccion.nombre
        ViewCompat.setTooltipText(holder.tvNombre, coleccion.nombre)
        
        if (coleccion.descripcion.isNullOrBlank()) {
            holder.tvDescripcion.visibility = View.GONE
        } else {
            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = coleccion.descripcion
        }

        // Estadísticas asociadas (si existen)
        val stats = coleccionStats[coleccion.id].orEmpty()
        if (stats.isBlank()) {
            holder.tvStats.visibility = View.GONE
            // Opcional: Si quieres mostrar "0" en lugar de ocultarlo:
            // holder.tvStats.visibility = View.VISIBLE
            // holder.tvStats.text = "0"
        } else {
            holder.tvStats.visibility = View.VISIBLE
            holder.tvStats.text = stats
        }

        // --- GESTIÓN DE COLOR DE COLECCIÓN ---
        val color = coleccion.color

        if (color != 0) {

            // Si la colección tiene color personalizado
            holder.colorDot.visibility = View.VISIBLE

            ViewCompat.setBackgroundTintList(
                holder.colorDot,
                ColorStateList.valueOf(color)
            )

            holder.card.strokeColor = color
            holder.card.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.5f, // Borde más acentuado
                holder.itemView.resources.displayMetrics
            ).toInt()

        } else {

            // Si no tiene color, se usa estilo por defecto
            holder.colorDot.visibility = View.GONE

            holder.card.strokeColor = MaterialColors.getColor(
                holder.card,
                com.google.android.material.R.attr.colorOutlineVariant
            )

            holder.card.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                holder.itemView.resources.displayMetrics
            ).toInt()
        }

        // --- CARGA DE IMAGEN ---
        val model = ImageUtils.toGlideModel(coleccion.imagenPath)

        if (model != null) {

            // Carga de imagen con Glide
            Glide.with(holder.itemView)
                .load(model)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .placeholder(R.drawable.ic_collection_default)
                .error(R.drawable.ic_collection_default)
                .into(holder.ivColeccion)

        } else {

            // Imagen por defecto si no hay imagen válida
            holder.ivColeccion.setImageResource(R.drawable.ic_collection_default)
        }
    }

    // Número total de elementos
    override fun getItemCount(): Int = colecciones.size

    // IDs estables para optimización del RecyclerView
    override fun getItemId(position: Int): Long =
        colecciones[position].id.toLong()

    /**
     * Actualiza la lista de colecciones utilizando [DiffUtil] para animaciones eficientes.
     * @param newList La nueva lista de [Coleccion].
     */
    fun updateList(newList: List<Coleccion>) {

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun getOldListSize(): Int = colecciones.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                colecciones[oldItemPosition].id == newList[newItemPosition].id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                colecciones[oldItemPosition] == newList[newItemPosition]
        })

        colecciones = newList

        // Aplica cambios calculados de forma animada
        diff.dispatchUpdatesTo(this)
    }

    // Devuelve un elemento concreto por posición
    fun getItem(position: Int): Coleccion = colecciones[position]

    // Fuerza actualización visual de una colección concreta por ID
    fun notifyStatsChangedFor(coleccionId: Int) {
        val index = colecciones.indexOfFirst { it.id == coleccionId }
        if (index != -1) notifyItemChanged(index)
    }
}
