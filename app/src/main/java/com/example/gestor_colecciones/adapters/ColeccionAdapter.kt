package com.example.gestor_colecciones.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Coleccion
import java.io.File

class ColeccionAdapter(
    private var colecciones: List<Coleccion>,
    private val onClick: (Coleccion) -> Unit,
    private val onLongClick: (Coleccion) -> Unit,
    private val coleccionStats: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ColeccionAdapter.ColeccionViewHolder>() {

    inner class ColeccionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreColeccion)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionColeccion)
        val tvStats: TextView = itemView.findViewById(R.id.tvStatsColeccion)
        val ivColeccion: ImageView = itemView.findViewById(R.id.ivColeccion)

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

        // Cargar imagen desde ruta interna si existe
        coleccion.imagenPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                holder.ivColeccion.setImageBitmap(bitmap)
            } else {
                holder.ivColeccion.setImageResource(R.drawable.ic_collection_default)
            }
        } ?: run {
            // Imagen por defecto si no hay ruta
            holder.ivColeccion.setImageResource(R.drawable.ic_collection_default)
        }
    }

    override fun getItemCount(): Int = colecciones.size

    fun updateList(newList: List<Coleccion>) {
        colecciones = newList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Coleccion = colecciones[position]
}