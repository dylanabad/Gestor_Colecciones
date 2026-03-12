package com.example.gestor_colecciones.adapters
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.entities.Coleccion
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.example.gestor_colecciones.R

class ColeccionAdapter(
    private var colecciones: List<Coleccion>,
    private val onItemClick: (Coleccion) -> Unit
) : RecyclerView.Adapter<ColeccionAdapter.ColeccionViewHolder>() {

    inner class ColeccionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreColeccion)
        init {
            itemView.setOnClickListener {
                onItemClick(colecciones[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColeccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coleccion, parent, false)
        return ColeccionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColeccionViewHolder, position: Int) {
        holder.tvNombre.text = colecciones[position].nombre
    }

    override fun getItemCount() = colecciones.size

    fun updateList(newList: List<Coleccion>) {
        colecciones = newList
        notifyDataSetChanged()
    }
}