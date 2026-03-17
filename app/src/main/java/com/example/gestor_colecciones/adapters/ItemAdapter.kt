package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Item

class ItemAdapter(
    items: List<Item> = emptyList(),
    categoriasMap: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // Lista de items a mostrar
    private var items: List<Item> = items

    // Mapa de categorías (id -> nombre)
    var categoriasMap: Map<Int, String> = categoriasMap
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Listeners públicos para clicks
    var onItemClick: ((Item) -> Unit)? = null
    var onItemLongClick: ((Item) -> Unit)? = null

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_item_title)
        val value: TextView = view.findViewById(R.id.tv_item_value)
        val categoria: TextView = view.findViewById(R.id.tv_item_categoria)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(items[position])
                }
            }

            view.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(items[position])
                }
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

    override fun getItemCount(): Int = items.size

    // Actualizar lista de items
    fun updateList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Obtener item por posición
    fun getItem(position: Int): Item = items[position]
}