package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Item

class ItemAdapter(
    var items: List<Item>,
    private val categoriasMap: Map<Int, String>, // mapa categoriaId -> nombre
    private val onItemClick: ((Item) -> Unit)? = null,
    private val onItemLongClick: ((Item) -> Unit)? = null
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_item_title)
        val value: TextView = view.findViewById(R.id.tv_item_value)
        val categoria: TextView = view.findViewById(R.id.tv_item_categoria)

        init {
            view.setOnClickListener { onItemClick?.invoke(items[adapterPosition]) }
            view.setOnLongClickListener {
                onItemLongClick?.invoke(items[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.titulo
        holder.value.text = "Valor: ${item.valor} €"
        holder.categoria.text = categoriasMap[item.categoriaId] ?: "Sin categoría"
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Item = items[position]
}