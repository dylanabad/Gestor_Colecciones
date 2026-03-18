package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Item
import java.io.File

class ItemAdapter(
    items: List<Item> = emptyList(),
    categoriasMap: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<Item> = items

    init {
        setHasStableIds(true)
    }

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

        // Cargar imagen del item (ruta en almacenamiento interno)
        val imagePath = item.imagenPath
        val file = imagePath?.let { File(it) }
        if (file != null && file.exists()) {
            Glide.with(holder.itemView)
                .load(file)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .placeholder(R.drawable.ic_no_image)
                .error(R.drawable.ic_no_image)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_no_image)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.toLong()

    fun updateList(newItems: List<Item>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                items[oldItemPosition].id == newItems[newItemPosition].id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                items[oldItemPosition] == newItems[newItemPosition]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun getItem(position: Int): Item = items[position]
}
