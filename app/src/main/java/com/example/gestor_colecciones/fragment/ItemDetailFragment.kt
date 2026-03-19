package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailFragment : Fragment() {

    private var itemId: Int = 0
    private lateinit var viewModel: ItemViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getInt(ARG_ITEM_ID) ?: 0
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_item_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = ItemRepository(DatabaseProvider.getDatabase(requireContext()).itemDao())
        viewModel = ItemViewModelFactory(repo).create(ItemViewModel::class.java)

        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val tvValor = view.findViewById<TextView>(R.id.tvValor)
        val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
        val tvDescripcion = view.findViewById<TextView>(R.id.tvDescripcion)
        val tvCategoria = view.findViewById<TextView>(R.id.tvCategoria)
        val tvCalificacion = view.findViewById<TextView>(R.id.tvCalificacion)
        val ivImagen = view.findViewById<ImageView>(R.id.ivImagen)
        val rbRating = view.findViewById<RatingBar>(R.id.rbItemRating)

        var currentItem: Item? = null
        rbRating.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (!fromUser) return@setOnRatingBarChangeListener
            val item = currentItem ?: return@setOnRatingBarChangeListener
            val clamped = rating.coerceIn(0f, 5f)
            tvCalificacion.text = String.format(Locale.getDefault(), "%.1f", clamped)
            val updated = item.copy(calificacion = clamped)
            currentItem = updated
            viewModel.update(updated) {
                Snackbar.make(view, "Calificación actualizada", Snackbar.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val item: Item? = viewModel.getItemById(itemId)
            item?.let {
                currentItem = it
                tvTitulo.text = it.titulo
                tvValor.text = "Valor: ${it.valor}"
                tvEstado.text = "Estado: ${it.estado}"
                tvDescripcion.text = "Descripción: ${it.descripcion ?: "N/A"}"
                val rating = it.calificacion.coerceIn(0f, 5f)
                rbRating.rating = rating
                tvCalificacion.text = String.format(Locale.getDefault(), "%.1f", rating)

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                view.findViewById<TextView>(R.id.tvFecha).text = "Fecha: ${sdf.format(it.fechaAdquisicion)}"

                // Cargar imagen si existe
                val imagePath = it.imagenPath
                val file = imagePath?.let { path -> File(path) }
                if (file != null && file.exists()) {
                    Glide.with(this@ItemDetailFragment)
                        .load(file)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade(180))
                        .placeholder(R.drawable.ic_no_image)
                        .error(R.drawable.ic_no_image)
                        .into(ivImagen)
                } else {
                    ivImagen.setImageResource(R.drawable.ic_no_image) // icono por defecto
                }

                // Cargar categoría (si quieres mostrar nombre de categoría)
                val categoriaDao = DatabaseProvider.getDatabase(requireContext()).categoriaDao()
                val categoria = categoriaDao.getAllCategoriasOnce().find { cat -> cat.id == it.categoriaId }
                tvCategoria.text = "Categoría: ${categoria?.nombre ?: "Sin categoría"}"
            }
        }
    }

    companion object {
        private const val ARG_ITEM_ID = "item_id"

        fun newInstance(itemId: Int) = ItemDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ITEM_ID, itemId) }
        }
    }
}
