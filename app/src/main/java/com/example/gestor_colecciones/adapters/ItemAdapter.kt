package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.util.ImageUtils

// Adapter principal para mostrar la lista de Items en un RecyclerView
class ItemAdapter(
    items: List<Item> = emptyList(),                  // Lista inicial de items
    categoriasMap: Map<Int, String> = emptyMap()       // Mapa de categorías por ID
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // Lista interna de items
    private var items: List<Item> = items

    init {
        // Optimización: IDs estables para mejor rendimiento en RecyclerView
        setHasStableIds(true)
    }

    // Mapa de categorías (ID -> nombre), se actualiza dinámicamente
    var categoriasMap: Map<Int, String> = categoriasMap
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Mapa de tags asociados a cada item (itemId -> lista de tags)
    var tagsByItemId: Map<Int, List<String>> = emptyMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // Callback cuando se hace click en un item
    var onItemClick: ((Item) -> Unit)? = null

    // Callback cuando se hace long click en un item
    var onItemLongClick: ((Item) -> Unit)? = null

    // ViewHolder que contiene las vistas del layout del item
    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title: TextView = view.findViewById(R.id.tv_item_title)
        val value: TextView = view.findViewById(R.id.tv_item_value)
        val categoria: TextView = view.findViewById(R.id.tv_item_categoria)
        val estado: TextView = view.findViewById(R.id.tv_item_estado)
        val tags: TextView = view.findViewById(R.id.tv_item_tags)
        val image: ImageView = view.findViewById(R.id.ivItemImage)
        val ratingBar: RatingBar = view.findViewById(R.id.rb_item_rating)
        val ratingValue: TextView = view.findViewById(R.id.tv_item_rating_value)
        val favorite: ImageView = view.findViewById(R.id.iv_item_favorite)

        init {

            // Click normal en item
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(items[position])
                }
            }

            // Click largo en item
            view.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(items[position])
                }
                true
            }
        }
    }

    // Inflado del layout del item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ItemViewHolder(view)
    }

    // Vinculación de datos con la vista
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {

        val item = items[position]

        // --- DATOS BÁSICOS ---
        holder.title.text = item.titulo
        holder.value.text = "Valor: ${item.valor} €"
        holder.categoria.text = categoriasMap[item.categoriaId] ?: "Sin categoría"
        holder.estado.text = "Estado: ${item.estado}"

        // --- TAGS ---
        val tags = tagsByItemId[item.id].orEmpty()

        if (tags.isEmpty()) {

            holder.tags.visibility = View.GONE

        } else {

            holder.tags.visibility = View.VISIBLE

            holder.tags.text =
                "Etiquetas: " + tags.take(3).joinToString(" · ") +
                        (if (tags.size > 3) " (+${tags.size - 3})" else "")
        }

        // --- RATING ---
        val rating = item.calificacion.coerceIn(0f, 5f)

        holder.ratingBar.rating = rating

        holder.ratingValue.text =
            String.format(java.util.Locale.getDefault(), "%.1f", rating)

        // --- FAVORITO ---
        holder.favorite.visibility =
            if (item.favorito) View.VISIBLE else View.GONE

        // --- IMAGEN ---
        val model = ImageUtils.toGlideModel(item.imagenPath)

        if (model != null) {

            Glide.with(holder.itemView)
                .load(model)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(180))
                .placeholder(R.drawable.ic_no_image)
                .error(R.drawable.ic_no_image)
                .into(holder.image)

        } else {

            holder.image.setImageResource(R.drawable.ic_no_image)
        }
    }

    // Número total de items
    override fun getItemCount(): Int = items.size

    // ID estable del item (optimización RecyclerView)
    override fun getItemId(position: Int): Long =
        items[position].id.toLong()

    // Actualización eficiente usando DiffUtil
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

    // Devuelve un item por posición
    fun getItem(position: Int): Item = items[position]
}