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

class ColeccionAdapter(
    private var colecciones: List<Coleccion>,
    private val onClick: (Coleccion) -> Unit,
    private val onLongClick: (Coleccion) -> Unit,
    private val coleccionStats: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ColeccionAdapter.ColeccionViewHolder>() {

    init {
        setHasStableIds(true)
    }

    inner class ColeccionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardColeccion)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreColeccion)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionColeccion)
        val tvStats: TextView = itemView.findViewById(R.id.tvStatsColeccion)
        val ivColeccion: ImageView = itemView.findViewById(R.id.ivColeccion)
        val colorDot: View = itemView.findViewById(R.id.viewColeccionColor)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(colecciones[position])
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) onLongClick(colecciones[position])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColeccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coleccion, parent, false)
        return ColeccionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColeccionViewHolder, position: Int) {
        val coleccion = colecciones[position]
        holder.tvNombre.text = coleccion.nombre
        holder.tvDescripcion.text = coleccion.descripcion ?: "Sin descripción"
        holder.tvStats.text = coleccionStats[coleccion.id] ?: ""

        val color = coleccion.color
        if (color != 0) {
            holder.colorDot.visibility = View.VISIBLE
            ViewCompat.setBackgroundTintList(holder.colorDot, ColorStateList.valueOf(color))
            holder.card.strokeColor = color
            holder.card.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2f,
                holder.itemView.resources.displayMetrics
            ).toInt()
        } else {
            holder.colorDot.visibility = View.GONE
            holder.card.strokeColor = MaterialColors.getColor(holder.card, com.google.android.material.R.attr.colorOutline)
            holder.card.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                holder.itemView.resources.displayMetrics
            ).toInt()
        }

        val model = ImageUtils.toGlideModel(coleccion.imagenPath)
        if (model != null) {
            Glide.with(holder.itemView)
                .load(model)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .placeholder(R.drawable.ic_collection_default)
                .error(R.drawable.ic_collection_default)
                .into(holder.ivColeccion)
        } else {
            holder.ivColeccion.setImageResource(R.drawable.ic_collection_default)
        }
    }

    override fun getItemCount(): Int = colecciones.size

    override fun getItemId(position: Int): Long = colecciones[position].id.toLong()

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
        diff.dispatchUpdatesTo(this)
    }

    fun getItem(position: Int): Coleccion = colecciones[position]

    fun notifyStatsChangedFor(coleccionId: Int) {
        val index = colecciones.indexOfFirst { it.id == coleccionId }
        if (index != -1) notifyItemChanged(index)
    }
}
