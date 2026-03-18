package com.example.gestor_colecciones.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Item

class ItemAdapter(
    items: List<Item> = emptyList(),
    categoriasMap: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<Item> = items

    var categoriasMap: Map<Int, String> = categoriasMap
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemClick: ((Item) -> Unit)? = null
    var onItemLongClick: ((Item) -> Unit)? = null

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_item_title)
        val value: TextView = view.findViewById(R.id.tv_item_value)
        val categoria: TextView = view.findViewById(R.id.tv_item_categoria)
        val image: ImageView = view.findViewById(R.id.ivItemImage)
        val preview: ImageView = view.findViewById(R.id.ivItemPreview)

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

        // Cargar imagen del item
        if (!item.imagenPath.isNullOrEmpty()) {
            val uri = Uri.parse(item.imagenPath)
            holder.image.setImageURI(uri)
            holder.preview.visibility = View.VISIBLE
            holder.preview.setImageURI(uri)
        } else {
            holder.image.setImageResource(R.mipmap.ic_launcher)
            holder.preview.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Item = items[position]
}